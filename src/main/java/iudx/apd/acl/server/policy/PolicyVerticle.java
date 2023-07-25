package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.common.Constants.POLICY_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PolicyVerticle extends AbstractVerticle {
  private PostgresService postgresService;
  private static final Logger LOGGER = LogManager.getLogger(PolicyVerticle.class);

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   */
  @Override
    public void start(){
    /* Read the configuration and set the postgres client properties. */
    LOGGER.debug("Info : " + LOGGER.getName());

    postgresService = new PostgresService(config(),vertx);
    PolicyService policyService = new PolicyServiceImpl(postgresService,config());
        new ServiceBinder(vertx)
                .setAddress(POLICY_SERVICE_ADDRESS)
                .register(PolicyService.class, policyService);
    }

}
