package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.common.Constants.POLICY_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class PolicyVerticle extends AbstractVerticle {
    private PostgresService postgresService;
    private PolicyServiceImpl policyService;
    private DeletePolicy deletePolicy;
    private CreatePolicy createPolicy;
    private VerifyPolicy verifyPolicy;
    private GetPolicy getPolicy;
    private CatalogueClient catalogueClient;
    @Override
    public void start() {
        postgresService = new PostgresService(config(), vertx);
        deletePolicy = new DeletePolicy(postgresService);
        getPolicy = new GetPolicy(postgresService);
        catalogueClient = new CatalogueClient(config());
        createPolicy = new CreatePolicy(postgresService,catalogueClient);
        verifyPolicy = new VerifyPolicy(postgresService,catalogueClient);
        policyService = new PolicyServiceImpl(deletePolicy, createPolicy, getPolicy,verifyPolicy,config());
        new ServiceBinder(vertx)
                .setAddress(POLICY_SERVICE_ADDRESS)
                .register(PolicyService.class, policyService);
    }
}
