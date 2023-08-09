package iudx.apd.acl.server.policy;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.apd.acl.server.apiserver.util.User;
import net.sf.saxon.trans.SymbolicName;

@VertxGen
@ProxyGen
public interface PolicyService {

  /* factory method */
  @GenIgnore
  static PolicyService createProxy(Vertx vertx, String address) {
    return new PolicyServiceVertxEBProxy(vertx, address);
  }

  /* service operation */

  Future<JsonObject> createPolicy(JsonObject request, User user);

  Future<JsonObject> deletePolicy(JsonArray policyList, User user);

  Future<JsonObject> getPolicy(User user);
  Future<JsonObject> verifyPolicy(JsonObject jsonArray);
}
