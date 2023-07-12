package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.apd.acl.server.policy.utility.Constants.*;

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

  public Future<JsonObject> initiateDeletePolicy(JsonArray policyList, String ownerId) {
    promise = Promise.promise();
    policyIdSet = new HashSet<>();
    JsonObject policyJsonObject = new JsonObject();
    for (int index = 0; index < policyList.size(); index++) {
      JsonObject policy = policyList.getJsonObject(index);
      policyJsonObject.put(String.valueOf(index), policy.getString("id"));
      if (policyIdSet.contains(policy.getString("id"))) {
        LOG.error("Duplicate policy Ids");
        promise.fail(
            getFailureResponse(responseJson, "Duplicate policy Ids present in the request"));
      }
      policyIdSet.add(policy.getString("id"));
    }
    ownerId = "4e563a5f-35f0-4f32-92be-8830775a1c5e";

    query = COUNT_OF_ACTIVE_POLICIES.replace("$0", POLICY_TABLE_NAME).replace("$2", ownerId);
    finalQuery = DELETE_POLICY_QUERY.replace("$0", POLICY_TABLE_NAME);

    postgresService.executeQueryWithParams(
        query,
        policyJsonObject,
        handler -> {
          if (handler.succeeded()) {
            LOG.trace("inside success handler with the result : " + handler.result().encode());
            if (handler.result() != null
                && handler.result().getJsonArray("result").getJsonObject(0).getInteger("count")
                    != null) {
              int count =
                  handler.result().getJsonArray("result").getJsonObject(0).getInteger("count");
              LOG.trace(
                  "count from the result is : "
                      + count
                      + " | policyList size is : "
                      + policyList.size());
              if (count == policyList.size()) {
                postgresService.executeQueryWithParams(
                    finalQuery,
                    policyJsonObject,
                    updateQueryHandler -> {
                      if (updateQueryHandler.succeeded()) {
                        LOG.debug("update query succeeded");
                        responseJson = handler.result();
                        responseJson.put(STATUS_CODE, HttpStatusCode.SUCCESS.getValue());
                        promise.complete(responseJson);
                      } else {
                        LOG.debug("update query failed");
                        promise.tryFail(
                            getFailureResponse(
                                responseJson, FAILURE_MESSAGE + ", update query failed"));
                      }
                    });
              } else {
                LOG.error("count not equal to list of policies to be deleted");
                promise.tryFail(
                    getFailureResponse(
                        responseJson,
                        FAILURE_MESSAGE + ", count not equal to list of policies to be deleted"));
              }
            } else {
              LOG.error("Failure : " + handler.cause().getMessage());
              promise.tryFail(
                  getFailureResponse(
                      responseJson, FAILURE_MESSAGE + ", " + handler.cause().getMessage()));
            }
          } else {
            LOG.error("Query execution failed : " + handler.cause().getMessage());
            promise.tryFail(
                getFailureResponse(responseJson, FAILURE_MESSAGE + ", Query execution failed "));
          }
        });
    return promise.future();
  }

  private String getFailureResponse(JsonObject response, String detail) {
    return response
        .put(TYPE, BAD_REQUEST.getValue())
        .put(TITLE, BAD_REQUEST.getUrn())
        .put(DETAIL, detail)
        .encode();
  }
}
