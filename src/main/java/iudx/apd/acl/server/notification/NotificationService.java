package iudx.apd.acl.server.notification;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.policy.PolicyService;
import iudx.apd.acl.server.policy.PolicyServiceVertxEBProxy;

@VertxGen
@ProxyGen
public interface NotificationService {

  /* factory method */
  @GenIgnore
  static NotificationService createProxy(Vertx vertx, String address) {
    return new NotificationServiceVertxEBProxy(vertx, address);
  }

  /* service operation */

  Future<JsonObject> createNotification(JsonObject request, User user);

  Future<JsonObject> deleteNotification(JsonObject notification, User user);

  Future<JsonObject> getNotification(User user);

  Future<JsonObject> updateNotification(JsonObject request, User user);
}
