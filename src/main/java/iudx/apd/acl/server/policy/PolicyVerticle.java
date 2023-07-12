package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.common.Constants.POLICY_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class PolicyVerticle extends AbstractVerticle {
  private PostgresService postgresService;
  private PolicyServiceImpl policyService;
  private DeletePolicy deletePolicy;
  private CreatePolicy createPolicy;

  @Override
  public void start() {
    postgresService = new PostgresService(config(), vertx);
    deletePolicy = new DeletePolicy(postgresService);
    createPolicy = new CreatePolicy(postgresService);
    policyService = new PolicyServiceImpl(deletePolicy, createPolicy);
    new ServiceBinder(vertx)
        .setAddress(POLICY_SERVICE_ADDRESS)
        .register(PolicyService.class, policyService);
  }
}
