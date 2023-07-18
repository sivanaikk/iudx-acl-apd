package iudx.apd.acl.server.policy;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyServiceImpl implements PolicyService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyServiceImpl.class);
  private final JsonObject config;
  private final CreatePolicy createPolicyObj;

  public PolicyServiceImpl(PostgresService postgresService, JsonObject config) {
    this.config = config;
    this.createPolicyObj = new CreatePolicy(postgresService);
  }

  @Override
  public Future<JsonObject> createPolicy(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    request.put("defaultExpiryDays", config.getLong("defaultExpiryDays"));
    createPolicyObj
        .initiateCreatePolicy(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete(handler.result());
              } else {
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonObject> deletePolicy(JsonObject policyList) {
    //        Promise promise = Promise.promise();
    //        String query = "";
    //        PgPool pgPool = PgPool.pool();
    //        pgPool.
    //                withConnection(connection -> {
    //                    connection.query(query).collecting()
    //                })
    return null;
  }

  @Override
  public Future<JsonObject> getPolicy(JsonObject request) {
    return null;
  }
}
