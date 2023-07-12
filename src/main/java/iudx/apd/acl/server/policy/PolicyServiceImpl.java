package iudx.apd.acl.server.policy;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.apd.acl.server.database.PostgresService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static iudx.apd.acl.server.policy.utility.Constants.DELETE_POLICY_QUERY;

public class PolicyServiceImpl implements PolicyService {
  private static final Logger LOG = LoggerFactory.getLogger(PolicyServiceImpl.class);
  CreatePolicy createPolicy;
  DeletePolicy deletePolicy;
  public PolicyServiceImpl(Postgres postgres){
  this.createPolicy = new CreatePolicy(postgres);
  this.deletePolicy = new DeletePolicy(postgres);
  }
  @Override
  public Future<JsonObject> createPolicy(JsonObject request) {
    return null;
  }

  @Override
  public Future<JsonObject> deletePolicy(JsonObject request) {
    return null;
  }

  @Override
  public Future<JsonObject> getPolicy(JsonObject request) {
    return null;
  }
}
