package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.policy.utility.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.apd.acl.server.database.PostgresService;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeletePolicy {
  private static final Logger LOG = LoggerFactory.getLogger(DeletePolicy.class);
  private final PostgresService postgresService;

  public DeletePolicy(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  /* TODO : replace the hardcoded ownerId with the actual ownerId*/
  public Future<JsonObject> deletePolicy(JsonArray policyList, String ownerId) {

    Set<String> policyIdSet = new HashSet<>();
    Promise<JsonObject> promise = Promise.promise();
    StringBuilder policyIds = new StringBuilder();
    for (int index = 0; index < policyList.size(); index++) {
      JsonObject policy = policyList.getJsonObject(index);

      if (policyIdSet.contains(policy.getString("id"))) {
        System.out.println("Duplicate policy Ids");
        return Future.failedFuture("Duplicate policy Ids present in the request");
      }
      policyIdSet.add(policy.getString("id"));
      System.out.println("whats in the policyIdSet : " + policyIdSet);
      String policyId = "'" + policy.getString("id") + "',";
      policyIds.append(policyId);
    }
    String policies = policyIds.substring(0, policyIds.length() - 1);
    ownerId = "4e563a5f-35f0-4f32-92be-8830775a1c5e";

    String query =
        AMOUNT_OF_ACTIVE_POLICIES_OF_OWNER_QUERY
            .replace("$0", POLICY_TABLE_NAME)
            .replace("$1", policies)
            .replace("$2", ownerId);

    String finalQuery =
        DELETE_POLICY_QUERY.replace("$0", POLICY_TABLE_NAME).replace("$1", policies);

    postgresService.executeQuery(
        query,
        handler -> {
          if (handler.succeeded()) {
            if (handler.result().getInteger("count") == policyList.size()) {
              System.out.println("success");
              postgresService.executeDbQuery(
                  finalQuery,
                  updateQueryHandler -> {
                    if (updateQueryHandler.succeeded()) {
                      System.out.println("update query succeeded");
                      promise.complete(new JsonObject().put("result11", "update query succeeded"));
                    } else {
                      System.out.println("update query failed");
                      promise.fail("update query failed");
                    }
                  });
            } else {
              promise.fail("count not equal to list of policy");
              System.out.println("count not equal to list of policies to be deleted");
            }
          } else {
            promise.fail("first query failed");
            System.out.println("failed");
          }
        });
    return promise.future();
  }
}
