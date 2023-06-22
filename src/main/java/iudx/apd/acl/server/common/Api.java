package iudx.apd.acl.server.common;

import static iudx.apd.acl.server.apiserver.util.Constants.POLICIES_API;
import static iudx.apd.acl.server.apiserver.util.Constants.REQUEST_POLICY_API;

public class Api {

  private static volatile Api apiInstance;
  private StringBuilder policiesUrl;

  private StringBuilder requestPoliciesUrl;

  private Api() {
    buildPaths();
  }

  public static Api getInstance() {
    if (apiInstance == null) {
      synchronized (Api.class) {
        if (apiInstance == null) {
          apiInstance = new Api();
        }
      }
    }
    return apiInstance;
  }

  private void buildPaths() {
    policiesUrl = new StringBuilder(POLICIES_API);
    requestPoliciesUrl = new StringBuilder(REQUEST_POLICY_API);
  }

  public String getPoliciesUrl() {
    return policiesUrl.toString();
  }

  public String getRequestPoliciesUrl() {
    return requestPoliciesUrl.toString();
  }
}
