package iudx.apd.acl.server.apiserver;

import static iudx.apd.acl.server.apiserver.util.Constants.ALLOWED_HEADERS;
import static iudx.apd.acl.server.apiserver.util.Constants.ALLOWED_METHODS;
import static iudx.apd.acl.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.apd.acl.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.apd.acl.server.apiserver.util.Util.errorResponse;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import iudx.apd.acl.server.common.Api;
import iudx.apd.acl.server.common.HttpStatusCode;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The ACL-APD Server API Verticle.
 *
 * <h1>ACL-APD Server API Verticle</h1>
 *
 * <p>The API Server verticle implements the IUDX ACL-APD Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 *
 * @see io.vertx.core.Vertx
 * @see AbstractVerticle
 * @see HttpServer
 * @see Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @version 1.0
 * @since 2020-05-31
 */
public class ApiServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);

  private HttpServer server;
  private Router router;
  private int port;
  private boolean isSSL;
  private String dxApiBasePath;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, reads the
   * configuration, obtains a proxy for the Event bus services exposed through service discovery,
   * start an HTTPs server at port 8443 or an HTTP server at port 8080.
   *
   * @throws Exception which is a startup exception TODO Need to add documentation for all the
   */
  @Override
  public void start() throws Exception {

    /* Define the APIs, methods, endpoints and associated methods. */
    Api api = Api.getInstance();

    router = Router.router(vertx);
    configureCorsHandler(router);

    putCommonResponseHeaders();

    // attach custom http error responses to router
    configureErrorHandlers(router);

    router.route().handler(BodyHandler.create());
    router.route().handler(TimeoutHandler.create(10000, 408));

    /* Api endpoints */
    router.get(api.getPoliciesUrl()).handler(this::getPoliciesHandler);
    router.delete(api.getPoliciesUrl()).handler(this::deletePoliciesHandler);
    router.post(api.getPoliciesUrl()).handler(this::postPoliciesHandler);

    router.get(api.getRequestPoliciesUrl()).handler(this::getAccessRequestHandler);
    router.delete(api.getRequestPoliciesUrl()).handler(this::deleteAccessRequestHandler);
    router.post(api.getRequestPoliciesUrl()).handler(this::postAccessRequestHandler);
    router.put(api.getRequestPoliciesUrl()).handler(this::putAccessRequestHandler);

    /* Read ssl configuration. */
    HttpServerOptions serverOptions = new HttpServerOptions();
    setServerOptions(serverOptions);
    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    server = vertx.createHttpServer(serverOptions);
    server.requestHandler(router).listen(port);

    /* Print the deployed endpoints */
    LOGGER.info("API server deployed on: " + port);
  }

  private void putAccessRequestHandler(RoutingContext routingContext) {}

  private void postAccessRequestHandler(RoutingContext routingContext) {}

  private void deleteAccessRequestHandler(RoutingContext routingContext) {}

  private void getAccessRequestHandler(RoutingContext routingContext) {}

  private void postPoliciesHandler(RoutingContext routingContext) {}

  private void deletePoliciesHandler(RoutingContext routingContext) {

  }

  private void getPoliciesHandler(RoutingContext routingContext) {}

  /**
   * Configures the CORS handler on the provided router.
   *
   * @param router The router instance to configure the CORS handler on.
   */
  private void configureCorsHandler(Router router) {
    router
        .route()
        .handler(
            CorsHandler.create("*")
                .allowedHeaders(ALLOWED_HEADERS)
                .allowedMethods(ALLOWED_METHODS));
  }

  /**
   * Configures error handlers for the specified status codes on the provided router.
   *
   * @param router The router instance to configure the error handlers on.
   */
  private void configureErrorHandlers(Router router) {
    HttpStatusCode[] statusCodes = HttpStatusCode.values();
    Stream.of(statusCodes)
        .forEach(
            code -> {
              router.errorHandler(
                  code.getValue(),
                  errorHandler -> {
                    HttpServerResponse response = errorHandler.response();
                    if (response.headWritten()) {
                      try {
                        response.close();
                      } catch (RuntimeException e) {
                        LOGGER.error("Error: " + e);
                      }
                      return;
                    }
                    response
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(code.getValue())
                        .end(errorResponse(code));
                  });
            });
  }

  /** Sets common response headers to be included in HTTP responses. */
  private void putCommonResponseHeaders() {
    router
        .route()
        .handler(
            requestHandler -> {
              requestHandler
                  .response()
                  .putHeader("Cache-Control", "no-cache, no-store,  must-revalidate,max-age=0")
                  .putHeader("Pragma", "no-cache")
                  .putHeader("Expires", "0")
                  .putHeader("X-Content-Type-Options", "nosniff");
              requestHandler.next();
            });
  }

  /**
   * Sets the server options based on the configuration settings. If SSL is enabled, starts an HTTPS
   * server with the specified HTTP port. If SSL is disabled, starts an HTTP server with the
   * specified HTTP port. If the HTTP port is not specified in the configuration, default ports
   * (8080 for HTTP and 8443 for HTTPS) will be used.
   *
   * @param serverOptions The server options to be configured.
   */
  private void setServerOptions(HttpServerOptions serverOptions) {
    isSSL = config().getBoolean("ssl");
    if (isSSL) {
      LOGGER.debug("Info: Starting HTTPs server");
      port = config().getInteger("httpPort") == null ? 8443 : config().getInteger("httpPort");
    } else {
      LOGGER.debug("Info: Starting HTTP server");
      serverOptions.setSsl(false);
      port = config().getInteger("httpPort") == null ? 8080 : config().getInteger("httpPort");
    }
  }
}
