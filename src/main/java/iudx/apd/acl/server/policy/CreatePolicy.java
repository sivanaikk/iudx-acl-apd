package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.DETAIL;
import static iudx.apd.acl.server.apiserver.util.Constants.TITLE;
import static iudx.apd.acl.server.apiserver.util.Constants.TYPE;
import static iudx.apd.acl.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.apd.acl.server.common.HttpStatusCode.CONFLICT;
import static iudx.apd.acl.server.common.HttpStatusCode.FORBIDDEN;
import static iudx.apd.acl.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static iudx.apd.acl.server.policy.util.Constants.CHECK_EXISTING_POLICY;
import static iudx.apd.acl.server.policy.util.Constants.CREATE_POLICY_QUERY;
import static iudx.apd.acl.server.policy.util.Constants.ENTITY_TABLE_CHECK;
import static iudx.apd.acl.server.policy.util.Constants.INSERT_ENTITY_TABLE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.apiserver.util.ResourceObj;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.policy.util.Status;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreatePolicy {
  private static final Logger LOGGER = LogManager.getLogger(CreatePolicy.class);
  private final PostgresService postgresService;
  private final CatalogueClient catalogueClient;

  public CreatePolicy(PostgresService postgresService, CatalogueClient catalogueClient) {
    this.postgresService = postgresService;
    this.catalogueClient = catalogueClient;
  }

  public Future<JsonObject> initiateCreatePolicy(JsonObject request, User user) {
    Promise<JsonObject> promise = Promise.promise();
    JsonArray policyList = request.getJsonArray("request");
    UUID userId = UUID.fromString(user.getUserId());
    List<CreatePolicyRequest> createPolicyRequestList =
      CreatePolicyRequest.jsonArrayToList(policyList, request.getLong("defaultExpiryDays"));

    Set<UUID> itemIdList =
      createPolicyRequestList.stream()
        .map(CreatePolicyRequest::getItemId)
        .collect(Collectors.toSet());

    Future<Set<UUID>> checkIfItemPresent = checkForItemsInDb(itemIdList);

    Future<Boolean> isPolicyAlreadyExist =
      checkIfItemPresent.compose(
        providerIdSet -> {
          if (providerIdSet.size() == 1 && providerIdSet.contains(userId)) {
            return checkExistingPoliciesForId(createPolicyRequestList, userId);
          } else {
            LOGGER.error("Item does not belong to the policy creator.");
            return Future.failedFuture(generateErrorResponse(FORBIDDEN, "Ownership Error."));
          }
        });

    Future<JsonObject> insertPolicy =
      isPolicyAlreadyExist.compose(
        policyDoesNotExist -> {
          return createPolicy(createPolicyRequestList, userId)
            .compose(
              createPolicySuccessHandler -> {
                JsonArray responseArray = createResponseArray(createPolicySuccessHandler);
                JsonObject responseJson =
                  new JsonObject()
                    .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                    .put("title", ResponseUrn.SUCCESS_URN.getMessage())
                    .put("result", responseArray);
                return Future.succeededFuture(responseJson);
              });
        });

    insertPolicy.onSuccess(promise::complete).onFailure(promise::fail);
    return promise.future();
  }

  private Future<Set<UUID>> checkForItemsInDb(Set<UUID> itemIdList) {
    Promise<Set<UUID>> promise = Promise.promise();

    postgresService
      .getPool()
      .withConnection(
        sqlConnection ->
          sqlConnection
            .preparedQuery(ENTITY_TABLE_CHECK)
            .execute(Tuple.of(itemIdList.toArray(UUID[]::new)))
            .onFailure(
              existingIdFailureHandler -> {
                LOGGER.error(
                  "checkForItemsInDb db fail {}",
                  existingIdFailureHandler.getLocalizedMessage());
                promise.fail(
                  generateErrorResponse(
                    INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getDescription()));
              })
            .onSuccess(
              existingIdSuccessHandler -> {
                Set<UUID> providerIdSet = new HashSet<>();
                Set<UUID> existingItemIds = new HashSet<>();
                if (existingIdSuccessHandler.size() > 0) {
                  for (Row row : existingIdSuccessHandler) {
                    providerIdSet.add(row.getUUID("provider_id"));
                    existingItemIds.add(row.getUUID("_id"));
                  }
                  itemIdList.removeAll(existingItemIds);
                }
                if (!itemIdList.isEmpty()) {
                  Future<List<ResourceObj>> resourceObjList =
                    catalogueClient.fetchItems(itemIdList);
                  Future<Set<UUID>> providerIdsFromCat =
                    resourceObjList.compose(this::insertItemsIntoDb);
                  providerIdsFromCat
                    .onSuccess(
                      insertItemsSuccessHandler -> {
                        providerIdSet.addAll(insertItemsSuccessHandler);
                        promise.complete(providerIdSet);
                      })
                    .onFailure(
                      insertItemsFailureHandler -> {
                        promise.fail(
                          generateErrorResponse(
                            BAD_REQUEST, BAD_REQUEST.getDescription()));
                      });
                } else {
                  promise.complete(providerIdSet);
                }
              }));
    return promise.future();
  }

  private Future<Set<UUID>> insertItemsIntoDb(List<ResourceObj> resourceObjList) {
    Promise<Set<UUID>> promise = Promise.promise();
    List<Tuple> batch = new ArrayList<>();
    Set<UUID> providerIdSet =
      resourceObjList.stream().map(ResourceObj::getProviderId).collect(Collectors.toSet());

    for (ResourceObj resourceObj : resourceObjList) {
      UUID id = resourceObj.getItemId();
      UUID provider = resourceObj.getProviderId();
      UUID resourceGroupId = resourceObj.getResourceGroupId();
      batch.add(Tuple.of(id, provider, resourceGroupId));
    }

    postgresService
      .getPool()
      .withConnection(
        sqlConnection ->
          sqlConnection
            .preparedQuery(INSERT_ENTITY_TABLE)
            .executeBatch(batch)
            .onFailure(
              dbHandler -> {
                LOGGER.error("insertItemsIntoDb db fail " + dbHandler.getLocalizedMessage());
              })
            .onSuccess(
              dbSuccessHandler -> {
                promise.complete(providerIdSet);
              }));
    return promise.future();
  }

  private Future<Boolean> checkExistingPoliciesForId(
    List<CreatePolicyRequest> createPolicyRequestList, UUID providerId) {
    List<Tuple> selectTuples =
      createPolicyRequestList.stream()
        .map(
          createPolicyRequest ->
            Tuple.of(
              createPolicyRequest.getItemId(),
              createPolicyRequest.getItemType(),
              providerId,
              Status.ACTIVE,
              createPolicyRequest.getUserEmail()))
        .collect(Collectors.toList());
    Promise<Boolean> promise = Promise.promise();
    postgresService
      .getPool()
      .withTransaction(
        conn ->
          conn.preparedQuery(CHECK_EXISTING_POLICY)
            .executeBatch(selectTuples)
            .onFailure(
              failureHandler -> {
                LOGGER.error(
                  "isPolicyForIdExist fail :: " + failureHandler.getLocalizedMessage());
                promise.fail(
                  generateErrorResponse(
                    INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getDescription()));
              })
            .onSuccess(
              policyExists -> {
                if (policyExists.size() > 0) {
                  List<UUID> responseArray = new ArrayList<>();
                  for (RowSet<Row> rowSet = policyExists;
                       rowSet != null;
                       rowSet = rowSet.next()) {
                    rowSet.forEach(row -> responseArray.add(row.getUUID("_id")));
                  }
                  LOGGER.error("Policy already Exist.");
                  promise.fail(
                    generateErrorResponse(
                      CONFLICT,
                      "Policy already exist for some of the request objects "
                        + responseArray));
                } else {
                  promise.complete(false);
                }
              }));

    return promise.future();
  }

  Future<RowSet<Row>> createPolicy(List<CreatePolicyRequest> createPolicyRequestList, UUID userId) {
    Promise<RowSet<Row>> promise = Promise.promise();
    List<Tuple> createPolicyTuple =
      createPolicyRequestList.stream()
        .map(
          createPolicyRequest ->
            Tuple.of(
              createPolicyRequest.getUserEmail(),
              createPolicyRequest.getItemId(),
              userId,
              createPolicyRequest.getItemType(),
              createPolicyRequest.getExpiryTime(),
              createPolicyRequest.getConstraints()))
        .collect(Collectors.toList());

    postgresService
      .getPool()
      .withTransaction(
        conn -> {
          // Execute the batch query to create policies
          return conn.preparedQuery(CREATE_POLICY_QUERY)
            .executeBatch(createPolicyTuple)
            .onFailure(
              failureHandler -> {
                LOGGER.error(
                  "createPolicy fail :: " + failureHandler.getLocalizedMessage());
                // Fail the promise with an error response
                promise.fail(
                  generateErrorResponse(
                    INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR.getDescription()));
              })
            .onSuccess(promise::complete);
        });

    return promise.future();
  }

  private String generateErrorResponse(HttpStatusCode httpStatusCode, String errorMessage) {
    return new JsonObject()
      .put(TYPE, httpStatusCode.getValue())
      .put(TITLE, httpStatusCode.getUrn())
      .put(DETAIL, errorMessage)
      .encode();
  }

  private JsonArray createResponseArray(RowSet<Row> rows) {
    JsonArray response = new JsonArray();
    final JsonObject[] ownerJsonObject = {null};

    for (RowSet<Row> rowSet = rows; rowSet != null; rowSet = rowSet.next()) {
      rowSet.forEach(
        row -> {
          JsonObject jsonObject =
            new JsonObject()
              .put("policyId", row.getUUID("_id").toString())
              .put("userEmailId", row.getString("user_emailid"))
              .put("itemId", row.getUUID("item_id").toString())
              .put("expiryAt", row.getLocalDateTime("expiry_at").toString());

          if (ownerJsonObject[0] == null) {
            ownerJsonObject[0] =
              new JsonObject().put("ownerId", row.getValue("owner_id").toString());
          }
          response.add(jsonObject);
        });
    }

    response.add(ownerJsonObject[0]);
    return response;
  }
}
