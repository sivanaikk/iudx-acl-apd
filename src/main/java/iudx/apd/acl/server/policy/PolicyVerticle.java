package iudx.apd.acl.server.policy;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class PolicyVerticle extends AbstractVerticle {
    private static final String POLICY_SERVICE_ADDRESS = "iudx.acl.apd.server.policy.service";

    @Override
    public void start(){
        PolicyService policyService = new PolicyServiceImpl();
        new ServiceBinder(vertx)
                .setAddress(POLICY_SERVICE_ADDRESS)
                .register(PolicyService.class, policyService);
    }

}
