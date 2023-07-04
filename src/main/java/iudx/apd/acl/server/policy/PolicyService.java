package iudx.apd.acl.server.policy;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Vertx;
import jakarta.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.Future;

@VertxGen
@ProxyGen
public interface PolicyService {

  /* factory method */
  @GenIgnore
  static PolicyService createProxy(Vertx vertx, String address) {
    return new PolicyServiceVertxEBProxy(vertx, address);
  }

  /* service operation */

    Future<JsonObject> createPolicy(JsonObject request);

    Future<JsonObject> deletePolicy(JsonObject policyList);

    Future<JsonObject> getPolicy(JsonObject request);

}
