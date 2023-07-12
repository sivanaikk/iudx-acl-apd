package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.apd.acl.server.policy.utility.Constants.*;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.database.PostgresService;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeletePolicy {
  private static final Logger LOG = LoggerFactory.getLogger(DeletePolicy.class);
  private static final String FAILURE_MESSAGE = "Policy could not be deleted";
  private final PostgresService postgresService;
  private Promise<JsonObject> promise;
  private JsonObject responseJson;
  private Set<String> policyIdSet;
  private String query;
  private String finalQuery;

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

  private Future<Boolean> executeCountQuery(
      String query, int policyIdSetSize, JsonObject policyJsonObject) {
    Promise<Boolean> promise = Promise.promise();
    postgresService.executeQueryWithParams(
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

  private Future<JsonObject> executeUpdateQuery(String query, JsonObject policyJsonObject) {
    Promise<JsonObject> promise = Promise.promise();
    postgresService.executeQueryWithParams(
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
    var countQueryFuture = executeCountQuery(query, policyIdSet.size(), policyJsonObject);
    var updateQueryFuture = executeUpdateQuery(finalQuery, policyJsonObject);
    var resultFuture =
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
