package iudx.apd.acl.server.policy;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

import static iudx.apd.acl.server.common.Constants.POLICY_SERVICE_ADDRESS;

public class PolicyVerticle extends AbstractVerticle {

    @Override
    public void start(){
        PolicyService policyService = new PolicyServiceImpl();
        new ServiceBinder(vertx)
                .setAddress(POLICY_SERVICE_ADDRESS)
                .register(PolicyService.class, policyService);
    }

}
