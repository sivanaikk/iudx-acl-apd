package iudx.apd.acl.server.metering.databroker;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;


public interface DataBrokerService {
  Future<Void> publishMessage(String toExchange,
                              String routingKey,
                              JsonObject body);
}