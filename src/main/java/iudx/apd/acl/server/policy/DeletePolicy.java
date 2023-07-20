package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.apd.acl.server.policy.util.Constants.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.apiserver.util.User;
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
  private Set<UUID> policyIdSet;
  private String query;
  private String finalQuery;
  private PgPool pool;

  public DeletePolicy(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  private String getFailureResponse(JsonObject response, String detail) {
    return response
        .put(TYPE, BAD_REQUEST.getValue())
        .put(TITLE, BAD_REQUEST.getUrn())
        .put(DETAIL, detail)
        .encode();
  }

  /**
   * Executes delete policy by setting the status field in record to DELETED from ACTIVE
   *
   * @param query SQL query to update the status of the policy
   * @param policyUuid policy list as array of UUID
   * @return The response of the query execution
   */
  private Future<JsonObject> executeUpdateQuery(String query, UUID[] policyUuid) {
    LOG.info("inside executeUpdateQuery");
    Promise<JsonObject> promise = Promise.promise();
    Tuple tuple = Tuple.of(policyUuid);
    this.executeQuery(
        query,
        tuple,
        handler -> {
          if (handler.succeeded()) {
            LOG.debug("update query succeeded");
            JsonObject responseJson = handler.result();
            responseJson.put(STATUS_CODE, HttpStatusCode.SUCCESS.getValue());
            promise.complete(responseJson);
          } else {
            LOG.debug("update query failed");
            promise.fail(
                getFailureResponse(new JsonObject(), FAILURE_MESSAGE + ", update query failed"));
          }
        });
    return promise.future();
  }

  /**
   * Executes the respective queries
   *
   * @param query SQL Query to be executed
   * @param tuple exchangeables for the query
   * @param handler Result of the query execution is sent as Json Object in a handler
   */
  private void executeQuery(String query, Tuple tuple, Handler<AsyncResult<JsonObject>> handler) {

    pool = postgresService.getPool();
    Collector<Row, ?, List<JsonObject>> rowListCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());

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
                      .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                      .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                      .put(RESULT, response);
              handler.handle(Future.succeededFuture(responseJson));
            })
        .onFailure(
            failureHandler -> {
              LOG.error("Failure while executing the query : {}", failureHandler.getMessage());
              JsonObject response =
                  new JsonObject()
                      .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                      .put(TITLE, ResponseUrn.DB_ERROR_URN.getMessage())
                      .put(DETAIL, "Failure while executing query");
              handler.handle(Future.failedFuture(response.encode()));
            });
  }

  /**
   * Queries postgres table to check if the policy given in the request is owned by the provider or
   * provider delegate, Checks if the policy that is about to be deleted is ACTIVE or DELETED Checks
   * if the policy is expired for all the policy ids provided in the request If one of the policy id
   * fails any of the checks, it returns false
   *
   * @param query SQL query
   * @param policyIdSetSize number of policy ids'
   * @param policyUuid list of policies of UUID
   * @return true if qualifies all the checks
   */
  private Future<Boolean> executeCountQuery(
      User user, String query, int policyIdSetSize, UUID[] policyUuid) {
    LOG.info("inside executeCountQuery");
    Promise<Boolean> promise = Promise.promise();
    String ownerId = user.getUserId();
    LOG.info("What's the ownerId : " + ownerId);
    Tuple tuple = Tuple.of(policyUuid, ownerId);

    this.executeQuery(
        query,
        tuple,
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
                        new JsonObject(),
                        FAILURE_MESSAGE + ", count not equal to list of policies to be deleted"));
              }
            } catch (NullPointerException exception) {
              LOG.error("Failure : {} ", handler.cause().getMessage());
              promise.fail(getFailureResponse(new JsonObject(), FAILURE_MESSAGE));
            }
          } else {
            LOG.error("Query execution failed : {} ", handler.cause().getMessage());
            promise.fail(
                getFailureResponse(
                    new JsonObject(), FAILURE_MESSAGE + ", Query execution failed "));
          }
        });
    return promise.future();
  }
  /**
   * Acts as an entry point for count query and update query execution
   *
   * @param policyList Array list of policy ids to be deleted
   * @return result of the execution as Json Object
   */
  public Future<JsonObject> initiateDeletePolicy(JsonArray policyList, User user) {
    policyIdSet = new HashSet<>();
    Promise<JsonObject> promise = Promise.promise();
    for (int index = 0; index < policyList.size(); index++) {
      JsonObject policy = policyList.getJsonObject(index);
      if (policyIdSet.contains(UUID.fromString(policy.getString("id")))) {
        LOG.error("Duplicate policy Ids");
        return Future.failedFuture(
            getFailureResponse(new JsonObject(), "Duplicate policy Ids present in the request"));
      }
      policyIdSet.add(UUID.fromString(policy.getString("id")));
    }
    query = COUNT_OF_ACTIVE_POLICIES;
    finalQuery = DELETE_POLICY_QUERY;
    UUID[] policyUuid = policyIdSet.toArray(UUID[]::new);
    executeCountQuery(user, query, policyIdSet.size(), policyUuid)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                executeUpdateQuery(finalQuery, policyUuid)
                    .onComplete(
                        successHandler -> {
                          if (successHandler.succeeded()
                              && successHandler.result() != null
                              && !successHandler.result().isEmpty()) {
                            JsonObject response = successHandler.result();
                            response.put(RESULT, "Policy deleted successfully");
                            promise.complete(response);
                          } else {
                            promise.fail(successHandler.cause().getMessage());
                          }
                        });
              } else {
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
    /*    Future<Boolean> countQueryFuture =
        executeCountQuery(user, query, policyIdSet.size(), policyUuid);
    // Do not update the policy if any of the policy id doesn't complete the checks
    Future<JsonObject> updateQueryFuture = Future.failedFuture(getFailureResponse(responseJson,FAILURE_MESSAGE + ", update query failed"));
    if(countQueryFuture.succeeded()){
      updateQueryFuture = executeUpdateQuery(finalQuery, policyUuid);
      }
    Future<JsonObject> finalUpdateQueryFuture = updateQueryFuture;
    Future<JsonObject> resultFuture =
        CompositeFuture.all(countQueryFuture, updateQueryFuture)
            .compose(
                object -> {
                  if (countQueryFuture.succeeded()) {
                    if (finalUpdateQueryFuture.result() != null
                        && !finalUpdateQueryFuture.result().isEmpty()) {
                      JsonObject response = finalUpdateQueryFuture.result();
                      response.put(RESULT, "Policy deleted successfully");
                      return Future.succeededFuture(finalUpdateQueryFuture.result());
                    } else {
                      return Future.failedFuture(finalUpdateQueryFuture.cause().getMessage());
                    }
                  } else {
                    return Future.failedFuture(countQueryFuture.cause().getMessage());
                  }
                });
    return resultFuture;*/
  }
}
