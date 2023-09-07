package iudx.apd.acl.server.authentication.authorization;

import iudx.apd.acl.server.common.Api;

public class AuthorizationContextFactory {

  public static AuthorizationStrategy create(IudxRole role, Api apis) {

    if (role == null) {
      throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }

    switch (role) {
      case PROVIDER: {
        return ProviderAuthStrategy.getInstance(apis);
      }
      case CONSUMER: {
        return ConsumerAuthStrategy.getInstance(apis);
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }

}
