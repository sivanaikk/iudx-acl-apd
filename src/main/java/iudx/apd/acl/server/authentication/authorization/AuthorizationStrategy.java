package iudx.apd.acl.server.authentication.authorization;

public interface AuthorizationStrategy {

  boolean isAuthorized(AuthorizationRequest authRequest);
}
