package iudx.apd.acl.server.validation;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import iudx.apd.acl.server.apiserver.util.RequestType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ValidationHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(ValidationHandler.class);
  private final Vertx vertx;
  private final RequestType requestType;
  private final ValidationHandlerFactory validationHandlerFactory;

  public ValidationHandler(
      Vertx vertx, RequestType requestType, ValidationHandlerFactory validationHandlerFactory) {
    this.requestType = requestType;
    this.vertx = vertx;
    this.validationHandlerFactory = validationHandlerFactory;
  }

  /**
   * This method collects all the request related information like path param, query param, body,
   * header to send to a Factory class method called {@link
   * iudx.apd.acl.server.validation.ValidationHandlerFactory#build(Vertx, RequestType, MultiMap,
   * MultiMap, JsonObject)} for the request specific validation
   *
   * @param routingContext Routing context object
   */
  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.trace("inside validation");
    MultiMap parameters = routingContext.request().params();
    Map<String, String> pathParams = routingContext.pathParams();
    RequestBody requestBody = routingContext.body();
    JsonObject body = null;
    if (requestBody != null && requestBody.asJsonObject() != null) {
      body = requestBody.asJsonObject().copy();
    }
    MultiMap headers = routingContext.request().headers();
    parameters.addAll(pathParams);
    List<Validator> validations =
        validationHandlerFactory.build(vertx, requestType, parameters, headers, body);

    for (Validator validator : Optional.ofNullable(validations).orElse(Collections.emptyList())) {
      LOGGER.info("Validator : {} ", validator.getClass().getName());
      validator.isValid();
    }
    routingContext.next();
  }
}
