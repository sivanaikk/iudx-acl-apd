package iudx.apd.acl.server.policy;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyServiceImpl implements PolicyService {
  private static final Logger LOG = LoggerFactory.getLogger(PolicyServiceImpl.class);
  private final DeletePolicy deletePolicy;
  private final CreatePolicy createPolicy;

  public PolicyServiceImpl(DeletePolicy deletePolicy, CreatePolicy createPolicy) {
    this.deletePolicy = deletePolicy;
    this.createPolicy = createPolicy;
  }

  @Override
  public Future<JsonObject> createPolicy(JsonObject request) {
    return null;
  }

  @Override
  public Future<JsonObject> deletePolicy(JsonArray policyList) {
    Promise<JsonObject> promise = Promise.promise();
    this.deletePolicy
        .initiateDeletePolicy(policyList, "some-owner-id")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOG.info("Successfully deleted the policy");
                promise.complete(handler.result());
              } else {
                LOG.error("Failed to delete the policy");
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> getPolicy(JsonObject request) {
    return null;
  }
}
