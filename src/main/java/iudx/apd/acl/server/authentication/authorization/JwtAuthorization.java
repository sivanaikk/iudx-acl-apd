package iudx.apd.acl.server.authentication.authorization;
public final class JwtAuthorization {
  private final AuthorizationStrategy authStrategy;

  public JwtAuthorization(final AuthorizationStrategy authStrategy) {
    this.authStrategy = authStrategy;
  }

  public boolean isAuthorized(AuthorizationRequest authRequest) {
    return authStrategy.isAuthorized(authRequest);
  }

}
