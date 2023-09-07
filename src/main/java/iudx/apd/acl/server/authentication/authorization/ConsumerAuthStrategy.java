package iudx.apd.acl.server.authentication.authorization;

import static iudx.apd.acl.server.authentication.authorization.Method.DELETE;
import static iudx.apd.acl.server.authentication.authorization.Method.GET;
import static iudx.apd.acl.server.authentication.authorization.Method.POST;

import iudx.apd.acl.server.common.Api;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsumerAuthStrategy implements AuthorizationStrategy {
  private static volatile ConsumerAuthStrategy instance;
  Map<String, List<AuthorizationRequest>> consumerAuthorizationRules = new HashMap<>();

  private ConsumerAuthStrategy(Api apis) {
    buildPermissions(apis);
  }

  public static ConsumerAuthStrategy getInstance(Api apis) {
    if (instance == null) {
      synchronized (ConsumerAuthStrategy.class) {
        if (instance == null) {
          instance = new ConsumerAuthStrategy(apis);
        }
      }
    }
    return instance;
  }

  private void buildPermissions(Api apis) {
    List<AuthorizationRequest> apiAccessList = new ArrayList<>();

    apiAccessList.add(new AuthorizationRequest(GET, apis.getPoliciesUrl()));

    apiAccessList.add(new AuthorizationRequest(GET, apis.getRequestPoliciesUrl()));
    apiAccessList.add(new AuthorizationRequest(POST, apis.getRequestPoliciesUrl()));
    apiAccessList.add(new AuthorizationRequest(DELETE, apis.getRequestPoliciesUrl()));
    consumerAuthorizationRules.put("api", apiAccessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest) {
    return consumerAuthorizationRules.get("api").contains(authRequest);
  }
}
