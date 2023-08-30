package iudx.apd.acl.server.authentication;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;

public class Authentication implements AuthenticationHandler {
  @Override
  public void handle(RoutingContext routingContext) {
    routingContext.next();
  }
}
