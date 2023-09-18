package iudx.apd.acl.server.authenticator;

import static iudx.apd.acl.server.apiserver.util.Constants.API_ENDPOINT;
import static iudx.apd.acl.server.apiserver.util.Constants.API_METHOD;
import static iudx.apd.acl.server.apiserver.util.Constants.HEADER_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import io.micrometer.core.ipc.http.HttpSender.Method;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.apiserver.util.Role;
import iudx.apd.acl.server.authentication.JwtAuthenticationServiceImpl;
import iudx.apd.acl.server.authentication.model.JwtData;
import iudx.apd.acl.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class JwtAuthServiceImplTest {

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthServiceImplTest.class);
  private static JsonObject authConfig;
  private static JwtAuthenticationServiceImpl jwtAuthenticationService;
  private static Api apis;

  @BeforeAll
  @DisplayName("Initialize Vertx and deploy Auth Verticle")
  static void init(Vertx vertx, VertxTestContext testContext) {
    authConfig = new JsonObject();
    authConfig.put("issuer", "authvertx.iudx.io");
    authConfig.put("apdURL", "acl-apd.iudx.io");
    apis = Api.getInstance("/ngsi-ld/v1");
    JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
    jwtAuthOptions.addPubSecKey(
        new PubSecKeyOptions()
            .setAlgorithm("ES256")
            .setBuffer(
                "-----BEGIN CERTIFICATE-----\n"
                    + "MIIBnDCCAT+gAwIBAgIEEC1BXTAMBggqhkjOPQQDAgUAMEIxCTAHBgNVBAYTADEJMAcGA1UECBMAMQkwBwYDVQQHEwAxCTAHBgNVBAoTADEJMAcGA1UECxMAMQkwBwYDVQQDEwAwHhcNMjMwNjA1MDUwODQ4WhcNMjQwNjA0MDUwODQ4WjBCMQkwBwYDVQQGEwAxCTAHBgNVBAgTADEJMAcGA1UEBxMAMQkwBwYDVQQKEwAxCTAHBgNVBAsTADEJMAcGA1UEAxMAMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAErVwOLOln7NhUdfZOQFfTOtJ62AgdKUeYZm8DgWCWJHaaXT95dipr94zJM+inSuqohVFCTxjajdTH8/O9JO43rKMhMB8wHQYDVR0OBBYEFNH2u8eeqj3509HAFJQS4F5NF4TQMAwGCCqGSM49BAMCBQADSQAwRgIhAL7zHYdN6PFTccFm1y07X0t2mJxNfgOaxihTi2tA9D8AAiEAomGmBvXA72X1gfhK3dhaDSd52BN1fUP/ALYNiyuXHg0=\n"
                    + "-----END CERTIFICATE-----"));
    jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);

    JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

    jwtAuthenticationService = new JwtAuthenticationServiceImpl(jwtAuth, authConfig, apis);

    LOGGER.info("Auth tests setup complete");

    testContext.completeNow();
  }

  @Test
  @DisplayName("success - allow access for identity -> consumer")
  public void allow4ConsumerIdentityToken(VertxTestContext testContext) {
    authConfig.put(API_METHOD, Method.GET);
    authConfig.put(API_ENDPOINT, apis.getPoliciesUrl());
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.consumerToken);
    jwtAuthenticationService
        .tokenIntrospect(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                testContext.failNow("invalid access");
              }
            });
  }

  @Test
  @DisplayName("success - allow access for identity -> consumer delegate")
  public void allow4ConsumerDelegateIdentityToken(VertxTestContext testContext) {
    authConfig.put(API_METHOD, Method.GET);
    authConfig.put(API_ENDPOINT, apis.getPoliciesUrl());
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.consumerDelegateToken);
    jwtAuthenticationService
        .tokenIntrospect(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject result = handler.result();
                assertEquals(Role.CONSUMER.getRole(), result.getString("role").toLowerCase());
                assertEquals(true, result.getBoolean("isDelegate"));
                testContext.completeNow();
              } else {
                testContext.failNow("invalid access");
              }
            });
  }

  @Test
  @DisplayName("success - allow access for identity -> provider")
  public void allow4ProviderIdentityToken(VertxTestContext testContext) {
    authConfig.put(API_METHOD, Method.GET);
    authConfig.put(API_ENDPOINT, apis.getPoliciesUrl());
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.providerToken);
    jwtAuthenticationService
        .tokenIntrospect(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                testContext.failNow("invalid access");
              }
            });
  }

  @Test
  @DisplayName("success - allow access for identity -> provider delegate")
  public void allow4ProviderDelegateIdentityToken(VertxTestContext testContext) {
    authConfig.put(API_METHOD, Method.GET);
    authConfig.put(API_ENDPOINT, apis.getRequestPoliciesUrl());
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.providerDelegateToken);
    jwtAuthenticationService
        .tokenIntrospect(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject result = handler.result();
                assertEquals(Role.PROVIDER.getRole(), result.getString("role").toLowerCase());
                assertEquals(true, result.getBoolean("isDelegate"));
                testContext.completeNow();
              } else {
                testContext.failNow("invalid access");
              }
            });
  }

  @Test
  @DisplayName("fail - not access to consumer for POST policy")
  public void invalidRequestOfConsumerPostPolicy(VertxTestContext testContext) {
    authConfig.put(API_METHOD, Method.POST);
    authConfig.put(API_ENDPOINT, apis.getPoliciesUrl());
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.consumerDelegateToken);
    jwtAuthenticationService
        .tokenIntrospect(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow("invalid access");
                testContext.completeNow();
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("fail - not access to consumer for DELETE policy")
  public void invalidRequestOfConsumerDeletePolicy(VertxTestContext testContext) {
    authConfig.put(API_METHOD, Method.DELETE);
    authConfig.put(API_ENDPOINT, apis.getPoliciesUrl());
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.consumerDelegateToken);
    jwtAuthenticationService
        .tokenIntrospect(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow("invalid access");
                testContext.completeNow();
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("fail - not access to provider for POST notification")
  public void invalidRequestOfProviderPostNotification(VertxTestContext testContext) {
    authConfig.put(API_METHOD, Method.POST);
    authConfig.put(API_ENDPOINT, apis.getRequestPoliciesUrl());
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.providerToken);
    jwtAuthenticationService
        .tokenIntrospect(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow("invalid access");
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("fail - not access to provider for DELETE notification")
  public void invalidRequestOfProviderDeleteNotification(VertxTestContext testContext) {
    authConfig.put(API_METHOD, Method.DELETE);
    authConfig.put(API_ENDPOINT, apis.getRequestPoliciesUrl());
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.providerToken);
    jwtAuthenticationService
        .tokenIntrospect(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow("invalid access");
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("decode invalid jwt")
  public void decodeJwtFailure(VertxTestContext testContext) {
    jwtAuthenticationService
        .decodeJwt(JwtTokenHelper.invalidToken)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow(handler.cause());
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("invalid token -> invalid issuer")
  public void invalidTokenIssuerInvalid(VertxTestContext vertxTestContext) {
    JwtData jwtData = new JwtData();
    jwtData.setSub("fd47486b-3497-4248-ac1e-082e4d37a66c");
    jwtData.setIss("wrong.issuer.io");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1886135512);
    jwtData.setIat(1686135512);
    jwtData.setIid("rs:rs.iudx.io");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api").add("sub")));

    jwtAuthenticationService
        .validateJwtAccess(jwtData)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("valid token");
              } else vertxTestContext.completeNow();
            });
  }

  @Test
  @DisplayName("invalid token -> aud value null")
  public void invalidTokenAudValueNull(VertxTestContext vertxTestContext) {
    JwtData jwtData = new JwtData();
    jwtData.setSub("fd47486b-3497-4248-ac1e-082e4d37a66c");
    jwtData.setIss("authvertx.iudx.io");
    jwtData.setAud(null);
    jwtData.setExp(1886135512);
    jwtData.setIat(1686135512);
    jwtData.setIid("rs:rs.iudx.io");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api").add("sub")));

    jwtAuthenticationService
        .validateJwtAccess(jwtData)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("valid token");
              } else vertxTestContext.completeNow();
            });
  }

  @Test
  @DisplayName("invalid token -> invalid aud value")
  public void invalidTokenInvalidIid(VertxTestContext vertxTestContext) {
    JwtData jwtData = new JwtData();
    jwtData.setSub("fd47486b-3497-4248-ac1e-082e4d37a66c");
    jwtData.setIss("authvertx.iudx.io");
    jwtData.setAud("wrong.aud.value");
    jwtData.setExp(1886135512);
    jwtData.setIat(1686135512);
    jwtData.setIid("rs:rs.iudx.io");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api").add("sub")));

    jwtAuthenticationService
        .validateJwtAccess(jwtData)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("valid token");
              } else vertxTestContext.completeNow();
            });
  }

  @Test
  @DisplayName("success - allow access for authToken")
  public void allow4AuthToken(VertxTestContext testContext) {
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.authToken);
    jwtAuthenticationService
        .tokenIntrospectForVerify(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                testContext.failNow("invalid access");
              }
            });
  }

  @Test
  @DisplayName("failure - wrong token in authToken")
  public void invalidAuthTokenFail(VertxTestContext testContext) {
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.consumerToken);
    jwtAuthenticationService
        .tokenIntrospectForVerify(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow("invalid access");
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("auth token failure - auth token can't have role or iid")
  public void invalidAuthTokenFailInvalid1(VertxTestContext testContext) {
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.authToken);
    authConfig.put("issuer", "wrongIssuer");

    JwtData mockedJwtData = mock(JwtData.class);
    when(mockedJwtData.getRole()).thenReturn("consumer");
    when(mockedJwtData.getIid()).thenReturn("someIid");

    JwtAuthenticationServiceImpl jwtAuthenticationServiceSpy = spy(jwtAuthenticationService);
    doReturn(Future.succeededFuture(mockedJwtData))
        .when(jwtAuthenticationServiceSpy)
        .decodeJwt(anyString());

    jwtAuthenticationServiceSpy
        .tokenIntrospectForVerify(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow("invalid access");
              } else {
                assertEquals("Cannot have role or iid in JWT", handler.cause().getMessage());
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("auth token failure - wrongIssuer")
  public void invalid4AuthTokenFailInvalid2(VertxTestContext testContext) {
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.authToken);

    JwtData mockedJwtData = mock(JwtData.class);
    when(mockedJwtData.getIss()).thenReturn("wrongIssuer");

    JwtAuthenticationServiceImpl jwtAuthenticationServiceSpy = spy(jwtAuthenticationService);
    doReturn(Future.succeededFuture(mockedJwtData))
        .when(jwtAuthenticationServiceSpy)
        .decodeJwt(anyString());

    jwtAuthenticationServiceSpy
        .tokenIntrospectForVerify(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow("invalid access");
              } else {
                assertEquals("Incorrect issuer value in JWT", handler.cause().getMessage());
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("auth token failure - no aud value")
  public void invalid4AuthTokenFailInvalid3(VertxTestContext testContext) {
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.authToken);

    JwtData mockedJwtData = mock(JwtData.class);
    when(mockedJwtData.getIss()).thenReturn("authvertx.iudx.io");
    when(mockedJwtData.getAud()).thenReturn("");

    JwtAuthenticationServiceImpl jwtAuthenticationServiceSpy = spy(jwtAuthenticationService);
    doReturn(Future.succeededFuture(mockedJwtData))
        .when(jwtAuthenticationServiceSpy)
        .decodeJwt(anyString());

    jwtAuthenticationServiceSpy
        .tokenIntrospectForVerify(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow("invalid access");
              } else {
                assertEquals("No audience value in JWT", handler.cause().getMessage());
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("auth token failure - incorrect aud value")
  public void invalid4AuthTokenFailInvalid4(VertxTestContext testContext) {
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.authToken);

    JwtData mockedJwtData = mock(JwtData.class);
    when(mockedJwtData.getIss()).thenReturn("authvertx.iudx.io");
    when(mockedJwtData.getAud()).thenReturn("wrongAud");

    JwtAuthenticationServiceImpl jwtAuthenticationServiceSpy = spy(jwtAuthenticationService);
    doReturn(Future.succeededFuture(mockedJwtData))
        .when(jwtAuthenticationServiceSpy)
        .decodeJwt(anyString());

    jwtAuthenticationServiceSpy
        .tokenIntrospectForVerify(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow("invalid access");
              } else {
                assertEquals("Incorrect audience value in JWT", handler.cause().getMessage());
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("auth token failure - incorrect sub value")
  public void invalid4AuthTokenFailInvalid5(VertxTestContext testContext) {
    authConfig.put(HEADER_TOKEN, JwtTokenHelper.authToken);

    JwtData mockedJwtData = mock(JwtData.class);
    when(mockedJwtData.getIss()).thenReturn("authvertx.iudx.io");
    when(mockedJwtData.getAud()).thenReturn("acl-apd.iudx.io");
    when(mockedJwtData.getSub()).thenReturn("incorrect sub");

    JwtAuthenticationServiceImpl jwtAuthenticationServiceSpy = spy(jwtAuthenticationService);
    doReturn(Future.succeededFuture(mockedJwtData))
        .when(jwtAuthenticationServiceSpy)
        .decodeJwt(anyString());

    jwtAuthenticationServiceSpy
        .tokenIntrospectForVerify(authConfig)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.failNow("invalid access");
              } else {
                assertEquals("Incorrect subject value in JWT", handler.cause().getMessage());
                testContext.completeNow();
              }
            });
  }
}
