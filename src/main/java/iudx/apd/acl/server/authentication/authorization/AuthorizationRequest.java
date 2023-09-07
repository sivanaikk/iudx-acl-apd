package iudx.apd.acl.server.authentication.authorization;

import java.util.Objects;

public final class AuthorizationRequest {

  private final Method method;
  private final String api;

  public AuthorizationRequest(final Method method, final String api) {
    this.method = method;
    this.api = api;
  }

  public Method getMethod() {
    return method;
  }

  public String getApi() {
    return api;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuthorizationRequest that = (AuthorizationRequest) o;
    return method == that.method && Objects.equals(api, that.api);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, api);
  }
}
