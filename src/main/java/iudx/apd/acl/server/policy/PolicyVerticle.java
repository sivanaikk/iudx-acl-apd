package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.common.Constants.PG_SERVICE_ADDRESS;
import static iudx.apd.acl.server.common.Constants.POLICY_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.apd.acl.server.database.PostgresService;

public class PolicyVerticle extends AbstractVerticle {
private PostgresService postgresService;
  @Override
  public void start() {

    postgresService = PostgresService.createProxy(vertx,PG_SERVICE_ADDRESS);
    PolicyService policyService = new PolicyServiceImpl(postgresService);
    new ServiceBinder(vertx)
        .setAddress(POLICY_SERVICE_ADDRESS)
        .register(PolicyService.class, policyService);
  }
}
