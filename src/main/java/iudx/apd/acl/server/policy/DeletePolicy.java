package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.apd.acl.server.policy.Constants.*;
import static iudx.apd.acl.server.policy.util.Constants.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.apiserver.response.RestResponse;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeletePolicy {
  private static final Logger LOG = LoggerFactory.getLogger(DeletePolicy.class);
  private static final String FAILURE_MESSAGE = "Policy could not be deleted";
  private final PostgresService postgresService;
  private JsonObject responseJson;
  private Set<String> policyIdSet;
  private String query;
  private String finalQuery;
  private PgPool pool;

  public DeletePolicy(PostgresService postgresService) {
    this.postgresService = postgresService;
    responseJson = new JsonObject();
  }

  /* TODO : replace the hardcoded ownerId with the actual ownerId */

  private String getFailureResponse(JsonObject response, String detail) {
    return response
        .put(TYPE, BAD_REQUEST.getValue())
        .put(TITLE, BAD_REQUEST.getUrn())
        .put(DETAIL, detail)
        .encode();
  }

  /**
   * Queries postgres table to check if the policy given in the request is owned by the provider or
   * provider delegate, Checks if the policy that is about to be deleted is ACTIVE or DELETED Checks
   * if the policy is expired for all the policy ids provided in the request If one of the policy id
   * fails any of the checks, it returns false
   *
   * @param query SQL query
   * @param policyIdSetSize number of policy ids'
   * @param policyJsonObject list of policies from the request body
   * @return true if qualifies all the checks
   */
  private Future<Boolean> executeCountQuery(
      String query, int policyIdSetSize, JsonObject policyJsonObject) {
    Promise<Boolean> promise = Promise.promise();
    this.executeQuery(
        query,
        policyJsonObject,
        handler -> {
          if (handler.succeeded()) {
            try {
              int count =
                  handler.result().getJsonArray("result").getJsonObject(0).getInteger("count");
              boolean areTheNumberOfPoliciesEqual = policyIdSetSize == count;
              if (areTheNumberOfPoliciesEqual) {
                promise.complete(true);
              } else {
                LOG.error("count not equal to list of policies to be deleted");
                promise.fail(
                    getFailureResponse(
                        responseJson,
                        FAILURE_MESSAGE + ", count not equal to list of policies to be deleted"));
              }
            } catch (NullPointerException exception) {
              LOG.error("Failure : " + handler.cause().getMessage());
              promise.fail(getFailureResponse(responseJson, FAILURE_MESSAGE));
            }
          } else {
            LOG.error("Query execution failed : " + handler.cause().getMessage());
            promise.fail(
                getFailureResponse(responseJson, FAILURE_MESSAGE + ", Query execution failed "));
          }
        });
    return promise.future();
  }

  /**
   * Executes delete policy by setting the status field in record to DELETED from ACTIVE
   *
   * @param query SQL query to update the status of the policy
   * @param policyJsonObject policy list as Json object
   * @return The response of the query execution
   */
  private Future<JsonObject> executeUpdateQuery(String query, JsonObject policyJsonObject) {
    Promise<JsonObject> promise = Promise.promise();
    this.executeQuery(
        query,
        policyJsonObject,
        handler -> {
          if (handler.succeeded()) {
            LOG.debug("update query succeeded");
            responseJson = handler.result();
            responseJson.put(STATUS_CODE, HttpStatusCode.SUCCESS.getValue());
            promise.complete(responseJson);
          } else {
            LOG.debug("update query failed");
            promise.fail(
                getFailureResponse(responseJson, FAILURE_MESSAGE + ", update query failed"));
          }
        });
    return promise.future();
  }

  /**
   * Executes the respective queries
   *
   * @param query SQL Query to be executed
   * @param params parameters for the query
   * @param handler Result of the query execution is sent as Json Object in a handler
   */
  private void executeQuery(
      String query, JsonObject params, Handler<AsyncResult<JsonObject>> handler) {

    pool = postgresService.getPool();
    Collector<Row, ?, List<JsonObject>> rowListCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());
    UUID[] policyUuid =
        params.stream()
            .map(e -> e.getValue())
            .map(s -> UUID.fromString((String) s))
            .toArray(UUID[]::new);
    Tuple tuple = Tuple.of(policyUuid);
    pool.withConnection(
            sqlConnection ->
                sqlConnection
                    .preparedQuery(query)
                    .collecting(rowListCollector)
                    .execute(tuple)
                    .map(rows -> rows.value()))
        .onSuccess(
            successHandler -> {
              JsonArray response = new JsonArray(successHandler);
              JsonObject responseJson =
                  new JsonObject()
                      .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                      .put("title", ResponseUrn.SUCCESS_URN.getMessage())
                      .put("result", response);
              handler.handle(Future.succeededFuture(responseJson));
            })
        .onFailure(
            failureHandler -> {
              LOG.error("Failure while executing the query : " + failureHandler.getMessage());
              RestResponse response =
                  new RestResponse.Builder()
                      .build(
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(),
                          ResponseUrn.DB_ERROR_URN.getUrn(),
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription(),
                          failureHandler.getMessage());
              handler.handle(Future.failedFuture(response.toJson().encode()));
            });
  }

  /**
   * Acts as an entry point for count query and update query execution
   *
   * @param policyList Array list of policy ids to be deleted
   * @param ownerId id of the provider
   * @return result of the execution as Json Object
   */
  public Future<JsonObject> initiateDeletePolicy(JsonArray policyList, String ownerId) {
    policyIdSet = new HashSet<>();
    JsonObject policyJsonObject = new JsonObject();
    for (int index = 0; index < policyList.size(); index++) {
      JsonObject policy = policyList.getJsonObject(index);
      policyJsonObject.put(String.valueOf(index), policy.getString("id"));
      if (policyIdSet.contains(policy.getString("id"))) {
        LOG.error("Duplicate policy Ids");
        return Future.failedFuture(
            getFailureResponse(responseJson, "Duplicate policy Ids present in the request"));
      }
      policyIdSet.add(policy.getString("id"));
    }
    ownerId = "4e563a5f-35f0-4f32-92be-8830775a1c5e";

    query = COUNT_OF_ACTIVE_POLICIES.replace("$0", POLICY_TABLE_NAME).replace("$2", ownerId);
    finalQuery = DELETE_POLICY_QUERY.replace("$0", POLICY_TABLE_NAME);
    Future<Boolean> countQueryFuture =
        executeCountQuery(query, policyIdSet.size(), policyJsonObject);
    Future<JsonObject> updateQueryFuture = executeUpdateQuery(finalQuery, policyJsonObject);
    Future<JsonObject> resultFuture =
        CompositeFuture.all(countQueryFuture, updateQueryFuture)
            .compose(
                object -> {
                  if (countQueryFuture.succeeded()) {
                    if (updateQueryFuture.result() != null
                        && !updateQueryFuture.result().isEmpty()) {
                      JsonObject response = updateQueryFuture.result();
                      response.put(RESULT, "Policy deleted successfully");
                      return Future.succeededFuture(updateQueryFuture.result());
                    } else {
                      return Future.failedFuture(updateQueryFuture.cause().getMessage());
                    }
                  } else {
                    return Future.failedFuture(countQueryFuture.cause().getMessage());
                  }
                });
    return resultFuture;
  }
}
