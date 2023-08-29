package iudx.apd.acl.server.authentication;

import static iudx.apd.acl.server.common.Constants.AUTH_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Authentication Verticle.
 *
 * <h1>Authentication Verticle</h1>
 *
 * <p>The Authentication Verticle implementation in the IUDX ACL-APD Server exposes the {@link
 * iudx.apd.acl.server.authenticator.AuthenticationService} over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class AuthenticationVerticle extends AbstractVerticle {

//  private static final Logger LOGGER = LogManager.getLogger(AuthenticationVerticle.class);
  private AuthenticationService jwtAuthenticationService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   *
   * @throws Exception which is a startup exception
   */
  @Override
  public void start() throws Exception {
    this.jwtAuthenticationService = new JwtAuthenticationServiceImpl();

    /* Publish the Authentication service with the Event Bus against an address. */
    binder = new ServiceBinder(vertx);

    consumer =
        binder
            .setAddress(AUTH_SERVICE_ADDRESS)
            .register(AuthenticationService.class, jwtAuthenticationService);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
