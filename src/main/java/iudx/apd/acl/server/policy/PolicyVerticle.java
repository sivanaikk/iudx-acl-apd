package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.common.Constants.POLICY_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class PolicyVerticle extends AbstractVerticle {
  private PostgresService postgresService;

  @Override
  public void start() {
    postgresService = new PostgresService(config(), vertx);
    PolicyService policyService = new PolicyServiceImpl(postgresService);
    new ServiceBinder(vertx)
        .setAddress(POLICY_SERVICE_ADDRESS)
        .register(PolicyService.class, policyService);
  }
}
