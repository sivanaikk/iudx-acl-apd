package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.DETAIL;
import static iudx.apd.acl.server.apiserver.util.Constants.TITLE;
import static iudx.apd.acl.server.apiserver.util.Constants.TYPE;
import static iudx.apd.acl.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.apd.acl.server.policy.util.Constants.CHECK_EXISTING_APD_POLICY;
import static iudx.apd.acl.server.policy.util.Constants.CHECK_OWNERSHIP;
import static iudx.apd.acl.server.policy.util.Constants.CHECK_RESOURCE_ENTITY_TABLE;
import static iudx.apd.acl.server.policy.util.Constants.CREATE_POLICY_QUERY;
import static iudx.apd.acl.server.policy.util.Constants.RETRIEVE_POLICY_CREATED;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.apiserver.response.RestResponse;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.policy.util.Status;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreatePolicy {
  private static final Logger LOGGER = LogManager.getLogger(CreatePolicy.class);
  private final PostgresService postgresService;
  private final JsonObject responseJson;

  public CreatePolicy(PostgresService postgresService) {
    this.responseJson = new JsonObject();
    this.postgresService = postgresService;
  }

  public Future<JsonObject> initiateCreatePolicy(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonArray policyList = request.getJsonArray("request");
    UUID userId = UUID.fromString(request.getString("userId"));
    List<CreatePolicyRequest> createPolicyRequestList =
        CreatePolicyRequest.jsonArrayToList(policyList, request.getLong("defaultExpiryDays"));

    Set<UUID> itemIdList =
        createPolicyRequestList.stream()
            .map(CreatePolicyRequest::getItemId)
            .collect(Collectors.toSet());

    Future<Boolean> checkIfItemPresent = checkForItemsInDb(itemIdList);
    Future<Boolean> checkForOwnershipOfItem = checkForOwnershipOfItem(itemIdList, userId);
    Future<Boolean> isPolicyAlreadyExist =
        checkExistingPoliciesForId(createPolicyRequestList, userId);

    CompositeFuture.all(checkIfItemPresent, checkForOwnershipOfItem, isPolicyAlreadyExist)
        .onSuccess(
            successHandler -> {
              boolean itemPresent = successHandler.resultAt(0);
              boolean ownershipCheck = successHandler.resultAt(1);
              boolean policyExists = successHandler.resultAt(2);

              if (itemPresent && ownershipCheck && !policyExists) {
                LOGGER.info("Trying to create policy");
                createPolicy(createPolicyRequestList, userId)
                    .onSuccess(
                        createPolicySuccessHandler -> {
                          JsonArray responseArray = createResponseArray(createPolicySuccessHandler);
                          JsonObject responseJson =
                              new JsonObject()
                                  .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                                  .put("title", ResponseUrn.SUCCESS_URN.getMessage())
                                  .put("result", responseArray);
                          LOGGER.info("Policy created successfully ");
                          promise.complete(responseJson);
                        })
                    .onFailure(
                        createPolicyFailureHandler -> {
                          LOGGER.error(
                              "Failed to create policy: "
                                  + createPolicyFailureHandler.getLocalizedMessage());
                          RestResponse response =
                              new RestResponse.Builder()
                                  .build(
                                      HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(),
                                      ResponseUrn.DB_ERROR_URN.getUrn(),
                                      HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription(),
                                      createPolicyFailureHandler.getMessage());
                          promise.fail(response.toString());
                        });
              } else {
                LOGGER.info("Policy cannot be created");
                promise.fail("Policy cannot be created");
              }
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(
                  "Failed to check policy conditions: " + failureHandler.getLocalizedMessage());
              promise.fail(failureHandler.getLocalizedMessage());
            });

    return promise.future();
  }

  private Future<Boolean> checkForItemsInDb(Set<UUID> itemIdList) {
    Promise<Boolean> promise = Promise.promise();
    LOGGER.info("Trying to get rs from item ids");

    Collector<Row, ?, List<UUID>> itemIdCollector =
        Collectors.mapping(row -> row.getUUID("_id"), Collectors.toList());

    postgresService
        .getPool()
        .withConnection(
            sqlConnection ->
                sqlConnection
                    .preparedQuery(CHECK_RESOURCE_ENTITY_TABLE)
                    .collecting(itemIdCollector)
                    .execute(Tuple.of(itemIdList.toArray(UUID[]::new)))
                    .onFailure(
                        obj -> {
                          LOGGER.error("checkUserExist db fail :: " + obj.getLocalizedMessage());
                          promise.fail(
                              getFailureResponse(
                                  responseJson,
                                  "check user in db failed because " + obj.getLocalizedMessage()));
                        })
                    .onSuccess(
                        successHandler -> {
                          List<UUID> existingItemIds = successHandler.value();
                          LOGGER.info("Item ids from db {}", existingItemIds);
                          existingItemIds.forEach(itemIdList::remove);
                          if (!itemIdList.isEmpty()) {
                            LOGGER.info("Remaining items in list: " + itemIdList);
                            // TODO: call for catalogue to check for the remaining item id
                            // below promise will depend on the result we get from cat PASSING it
                            // for NOW
                            promise.complete(true);
                          } else {
                            LOGGER.info("All items are present");
                            promise.complete(true);
                          }
                        }));
    return promise.future();
  }

  Future<Boolean> checkForOwnershipOfItem(Set<UUID> itemIdList, UUID userId) {
    Promise<Boolean> promise = Promise.promise();
    LOGGER.info("Trying to check ownership of items");
    Collector<Row, ?, Set<UUID>> providerIdCollector =
        Collectors.mapping(row -> row.getUUID("provider_id"), Collectors.toSet());

    postgresService
        .getPool()
        .withConnection(
            sqlConnection ->
                sqlConnection
                    .preparedQuery(CHECK_OWNERSHIP)
                    .collecting(providerIdCollector)
                    .execute(Tuple.of(itemIdList.toArray(UUID[]::new)))
                    .onFailure(
                        obj -> {
                          LOGGER.error(
                              "checkForOwnerShipOfItem db fail :: " + obj.getLocalizedMessage());
                          promise.fail(
                              getFailureResponse(
                                  responseJson,
                                  "checkForOwnerShipOfItem in db failed because "
                                      + obj.getLocalizedMessage()));
                        })
                    .onSuccess(
                        successHandler -> {
                          Set<UUID> providerIds = successHandler.value();
                          LOGGER.info("Provider ids from db {}", providerIds);
                          if (providerIds.size() == 1 && providerIds.contains(userId)) {
                            LOGGER.info("OwnerShip Check: True");
                            promise.complete(true);
                          } else {
                            LOGGER.info("Ownership Error");
                            promise.fail("Item does not belongs to the policy creator.");
                          }
                        }));
    return promise.future();
  }

  private Future<Boolean> checkExistingPoliciesForId(
      List<CreatePolicyRequest> createPolicyRequestList, UUID providerId) {
    LOGGER.info("Checking for existing policies");

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
                conn.preparedQuery(CHECK_EXISTING_APD_POLICY)
                    .executeBatch(selectTuples)
                    .map(rows -> rows.size() > 0)
                    .onFailure(
                        failureHandler -> {
                          LOGGER.error(
                              "isPolicyForIdExist fail :: " + failureHandler.getLocalizedMessage());
                          promise.fail(
                              getFailureResponse(
                                  responseJson,
                                  "isPolicyForIdExist in db failed because "
                                      + failureHandler.getLocalizedMessage()));
                        })
                    .onSuccess(
                        policyExists -> {
                          if (policyExists) {
                            LOGGER.error("Policy already Exist.");
                            promise.fail("Policy already Exist.");
                          } else {
                            LOGGER.info("Policy does not Exist.");
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

    Set<String> userEmailid =
        createPolicyRequestList.stream()
            .map(CreatePolicyRequest::getUserEmail)
            .collect(Collectors.toSet());
    Set<UUID> itemid =
        createPolicyRequestList.stream()
            .map(CreatePolicyRequest::getItemId)
            .collect(Collectors.toSet());

    Tuple retrivePolicyTuple =
        Tuple.of(userEmailid.toArray(String[]::new), itemid.toArray(UUID[]::new), userId);

    postgresService
        .getPool()
        .withTransaction(
            conn ->
                conn.preparedQuery(CREATE_POLICY_QUERY)
                    .executeBatch(createPolicyTuple)
                    .onFailure(
                        failureHandler -> {
                          LOGGER.error(
                              "createPolicy fail :: " + failureHandler.getLocalizedMessage());
                          promise.fail(
                              getFailureResponse(
                                  responseJson,
                                  "createPolicy in db failed because "
                                      + failureHandler.getLocalizedMessage()));
                        })
                    .onSuccess(
                        successHandler -> {
                          conn.preparedQuery(RETRIEVE_POLICY_CREATED)
                              .execute(retrivePolicyTuple)
                              .onFailure(
                                  queryFailureHandler -> {
                                    LOGGER.error(
                                        "Failed to retrieve inserted rows: "
                                            + queryFailureHandler.getLocalizedMessage());
                                    promise.fail(
                                        "Failed to retrieve inserted rows: "
                                            + queryFailureHandler.getLocalizedMessage());
                                  })
                              .onSuccess(
                                  querySuccessHandler -> {
                                    LOGGER.info(
                                        "Inserted rows retrieved. Count: "
                                            + querySuccessHandler.size());
                                    promise.complete(querySuccessHandler);
                                  });
                        }));

    return promise.future();
  }

  private String getFailureResponse(JsonObject response, String detail) {
    return response
        .put(TYPE, BAD_REQUEST.getValue())
        .put(TITLE, BAD_REQUEST.getUrn())
        .put(DETAIL, detail)
        .encode();
  }

  private JsonArray createResponseArray(RowSet<Row> rows) {
    JsonArray response = new JsonArray();
    JsonObject ownerJsonObject = null;

    for (Row row : rows) {
      JsonObject jsonObject =
          new JsonObject()
              .put("policy_id", row.getValue("_id").toString())
              .put("user_emailId", row.getString("user_emailid"))
              .put("item_id", row.getValue("item_id").toString())
              .put("expiry_at", row.getValue("expiry_at").toString());

      if (ownerJsonObject == null) {
        ownerJsonObject = new JsonObject().put("owner_id", row.getValue("owner_id").toString());
      }

      response.add(jsonObject);
    }

    response.add(ownerJsonObject);
    return response;
  }
}
