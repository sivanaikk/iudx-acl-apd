package iudx.apd.acl.server.authentication;

import static iudx.apd.acl.server.apiserver.util.Constants.API_ENDPOINT;
import static iudx.apd.acl.server.apiserver.util.Constants.API_METHOD;
import static iudx.apd.acl.server.authentication.Constants.AUD;
import static iudx.apd.acl.server.authentication.Constants.IS_DELEGATE;
import static iudx.apd.acl.server.authentication.Constants.ROLE;
import static iudx.apd.acl.server.authentication.Constants.USER_ID;
import static iudx.apd.acl.server.authentication.authorization.IudxRole.DELEGATE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import iudx.apd.acl.server.authentication.authorization.AuthorizationContextFactory;
import iudx.apd.acl.server.authentication.authorization.AuthorizationRequest;
import iudx.apd.acl.server.authentication.authorization.AuthorizationStrategy;
import iudx.apd.acl.server.authentication.authorization.IudxRole;
import iudx.apd.acl.server.authentication.authorization.JwtAuthorization;
import iudx.apd.acl.server.authentication.authorization.Method;
import iudx.apd.acl.server.authentication.model.JwtData;
import iudx.apd.acl.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JwtAuthenticationServiceImpl implements AuthenticationService {
  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);
  final JWTAuth jwtAuth;
  final String audience;
  final String issuer;
  final Api apis;
  final String apdURL;

  public JwtAuthenticationServiceImpl(final JWTAuth jwtAuth, final JsonObject config, Api apis) {
    this.jwtAuth = jwtAuth;
    this.audience = config.getString("audience");
    this.issuer = config.getString("issuer");
    this.apdURL = config.getString("apdURL");
    this.apis = apis;
  }

  @Override
  public Future<JsonObject> tokenIntrospect(JsonObject authenticationInfo) {
    Promise<JsonObject> promise = Promise.promise();
    String token = authenticationInfo.getString("token");
    ResultContainer resultContainer = new ResultContainer();

    Future<JwtData> jwtDecodeFuture = decodeJwt(token);
    jwtDecodeFuture
        .compose(
            decodeHandler -> {
              resultContainer.jwtData = decodeHandler;
              return validateJwtAccess(resultContainer.jwtData);
            })
        .compose(isValidJwtAccess -> validateAccess(resultContainer.jwtData, authenticationInfo))
        .onSuccess(promise::complete)
        .onFailure(
            failureHandler -> {
              LOGGER.error("error : " + failureHandler.getMessage());
              promise.fail(failureHandler.getLocalizedMessage());
            });

    return promise.future();
  }

  @Override
  public Future<Void> tokenIntrospectForVerify(JsonObject authenticationInfo) {
    Promise<Void> promise = Promise.promise();
    String token = authenticationInfo.getString("token");

    Future<JwtData> jwtDecodeFuture = decodeJwt(token);
    jwtDecodeFuture
        .onSuccess(
            jwtData -> {
              if (!(jwtData.getRole() == null || jwtData.getIid() == null)) {
                LOGGER.error("Cannot have role or iid in JWT");
                promise.fail("Cannot have role or iid in JWT");
              } else if (!(jwtData.getIss() != null && issuer.equalsIgnoreCase(jwtData.getIss()))) {
                LOGGER.error("Incorrect issuer value in JWT");
                promise.fail("Incorrect issuer value in JWT");
              } else if (jwtData.getAud().isEmpty()) {
                LOGGER.error("No audience value in JWT");
                promise.fail("No audience value in JWT");
              } else if (!jwtData.getAud().equalsIgnoreCase(apdURL)) {
                LOGGER.error("Incorrect audience value in JWT");
                promise.fail("Incorrect audience value in JWT");
              } else if (!jwtData.getSub().equalsIgnoreCase(jwtData.getIss())) {
                LOGGER.error("Incorrect subject value in JWT");
                promise.fail("Incorrect subject value in JWT");
              } else {
                promise.complete();
              }
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("FAIL to decode the token.");
              promise.fail(failureHandler.getLocalizedMessage());
            });

    return promise.future();
  }

  Future<Boolean> validateJwtAccess(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();
    if (!(jwtData.getIss() != null && issuer.equalsIgnoreCase(jwtData.getIss()))) {
      LOGGER.error("Incorrect issuer value in JWT");
      promise.fail("Incorrect issuer value in JWT");
    } else if (jwtData.getAud() == null) {
      LOGGER.error("No audience value in JWT");
      promise.fail("No audience value in JWT");
    } else if (!jwtData.getAud().equalsIgnoreCase(jwtData.getIid().split(":")[1])) {
      LOGGER.error("Incorrect audience value in JWT");
      promise.fail("Incorrect audience value in JWT");
    } else {
      promise.complete(true);
    }

    return promise.future();
  }

  Future<JsonObject> validateAccess(JwtData jwtData, JsonObject authInfo) {
    LOGGER.info("Authorization check started");
    Promise<JsonObject> promise = Promise.promise();
    Method method = Method.valueOf(authInfo.getString(API_METHOD));
    String api = authInfo.getString(API_ENDPOINT);
    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);

    IudxRole role = IudxRole.fromRole(jwtData);

    AuthorizationStrategy authStrategy = AuthorizationContextFactory.create(role, apis);
    LOGGER.info("strategy : " + authStrategy.getClass().getSimpleName());

    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    if (jwtAuthStrategy.isAuthorized(authRequest)) {
      JsonObject jsonResponse = new JsonObject();
      boolean isDelegate = jwtData.getRole().equalsIgnoreCase(DELEGATE.getRole());
      jsonResponse.put(USER_ID, isDelegate ? jwtData.getDid() : jwtData.getSub());
      jsonResponse.put(IS_DELEGATE, isDelegate);
      jsonResponse.put(ROLE, role);
      jsonResponse.put(AUD, jwtData.getAud());
      promise.complete(jsonResponse);
    } else {
      LOGGER.info("Failed in authorization check.");
      JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
      promise.fail(result.toString());
    }
    return promise.future();
  }

  Future<JwtData> decodeJwt(String jwtToken) {
    Promise<JwtData> promise = Promise.promise();
    TokenCredentials creds = new TokenCredentials(jwtToken);

    jwtAuth
        .authenticate(creds)
        .onSuccess(
            user -> {
              JwtData jwtData = new JwtData(user.principal());
              jwtData.setExp(user.get("exp"));
              jwtData.setIat(user.get("iat"));
              promise.complete(jwtData);
            })
        .onFailure(
            err -> {
              LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
              promise.fail(err.getMessage());
            });

    return promise.future();
  }

  final class ResultContainer {
    JwtData jwtData;
  }
}
