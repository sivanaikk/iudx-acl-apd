package iudx.apd.acl.server.metering;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface MeteringService {
  @GenIgnore
  static MeteringService createProxy(Vertx vertx, String address) {
    return new MeteringServiceVertxEBProxy(vertx, address);
  }

  Future<Void> insertMeteringValuesInRmq(JsonObject request);
}
