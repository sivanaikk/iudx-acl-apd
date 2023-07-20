package iudx.apd.acl.server.validation;


import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.apd.acl.server.apiserver.util.RequestType;
import iudx.apd.acl.server.validation.types.PolicyIdTypeValidator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ValidationHandlerFactory {
  private static final Logger LOGGER = LogManager.getLogger(ValidationHandlerFactory.class);

  public List<Validator> build(
      final Vertx vertx,
      final RequestType requestType,
      final MultiMap parameters,
      final MultiMap headers,
      final JsonObject body) {
    LOGGER.debug("build validations started for :" + requestType);
    LOGGER.debug("type :" + requestType);
    List<Validator> validator = null;

    switch (requestType) {
      case POLICY:
        {
          validator = getPolicyRequestValidations(parameters,body);
          break;
        }
      case NOTIFICATION:
        {
          validator = getNotificationRequestValidations(parameters);
          break;
        }
      default: break;
    }
    return null;
  }

  private List<Validator> getPolicyRequestValidations(MultiMap params, JsonObject body) {
    List<Validator> validators = new ArrayList<>();

//    validators.add(new PolicyIdTypeValidator(params.get("policy"), true));
    //    return validators;
    return null;
  }

  private List<Validator> getNotificationRequestValidations(MultiMap params) {
    return new ArrayList<>();
  }
  public void getPolicyIdSet(JsonArray ids)
  {
    Set<String> policyIdSet = new HashSet<>();
    for(int index = 0; index < ids.size() ; index++)
    {
      JsonObject temp = ids.getJsonObject(index);

    }
  }
}
