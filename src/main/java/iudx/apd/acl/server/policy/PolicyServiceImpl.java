package iudx.apd.acl.server.policy;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.apd.acl.server.database.PostgresService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static iudx.apd.acl.server.policy.utility.Constants.DELETE_POLICY_QUERY;

public class PolicyServiceImpl implements PolicyService {
  private static final Logger LOG = LoggerFactory.getLogger(PolicyServiceImpl.class);
  private final PostgresService postgresService;

  public PolicyServiceImpl(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  @Override
  public Future<JsonObject> createPolicy(JsonObject request) {
    return null;
  }

  @Override
  public Future<JsonObject> deletePolicy(JsonArray policyList) {
    System.out.println("What's in the policyList : " + policyList.encode());
    String dbTableName = "policy";
    String policyId = "231f6eca-6276-4993-bfeb-53cbbbba6f06";

    String query = DELETE_POLICY_QUERY.replace("$0", dbTableName).replace("$1", policyId);
    postgresService.executeDbQuery(
        query,
        handler -> {
          if (handler.succeeded()) {
            System.out.println("success");
          } else {
            System.out.println("failed");
          }
        });
    return null;
  }

  @Override
  public Future<JsonObject> getPolicy(JsonObject request) {
    return null;
  }
}
