package iudx.apd.acl.server.authorization;

import static iudx.apd.acl.server.apiserver.util.Constants.ROLE;
import static iudx.apd.acl.server.authentication.Constants.AUD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.Utility;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.authentication.AuthClient;
import iudx.apd.acl.server.authentication.AuthHandler;
import iudx.apd.acl.server.authentication.AuthenticationService;
import iudx.apd.acl.server.common.Api;
import iudx.apd.acl.server.policy.PostgresService;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@Testcontainers
public class TestAuthHandler {
  private static final Logger LOG = LoggerFactory.getLogger(TestAuthHandler.class);
  static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11");
  private static User owner;
  private static User consumer;
  @Mock HttpServerRequest httpServerRequest;
  @Mock MultiMap multiMapMock;
  @Mock HttpMethod httpMethod;
  @Mock Future<JsonObject> future;
  @Mock Future<Void> voidFuture;
  @Mock Void aVoid;
  @Mock AsyncResult<Void> voidAsyncResult;
  @Mock Throwable throwable;
  @Mock HttpServerResponse httpServerResponse;
  @Mock PgPool pgPool;
  private Utility utility;
  private AuthHandler authHandler;
  private JsonObject config;
  private Api api;
  private AuthenticationService authenticationService;
  private AuthClient client;
  private PostgresService postgresService;
  private RoutingContext routingContext;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    utility = new Utility();
    container.start();
    postgresService = utility.setUp(container);
    config = new JsonObject();
    api = Api.getInstance("/dx/apd/acl/v3");
    authenticationService = mock(AuthenticationService.class);
    client = mock(AuthClient.class);
    routingContext = mock(RoutingContext.class);
    lenient().when(routingContext.request()).thenReturn(httpServerRequest);
    lenient().when(httpServerRequest.headers()).thenReturn(multiMapMock);
    lenient().when(multiMapMock.get(anyString())).thenReturn("dummyToken one");
    lenient().when(httpServerRequest.method()).thenReturn(httpMethod);
    lenient().when(httpMethod.toString()).thenReturn("someMethod");
    lenient().when(httpServerRequest.path()).thenReturn(api.getVerifyUrl());
    lenient().when(authenticationService.tokenIntrospectForVerify(any())).thenReturn(voidFuture);
    //        lenient().when(voidFuture.onSuccess(any())).thenReturn(Future.succeededFuture());
    //        when(voidFuture.succeeded()).thenReturn(true);

    lenient().when(voidAsyncResult.succeeded()).thenReturn(true);
    lenient().doNothing().when(routingContext).next();

    lenient()
        .doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg2.getArgument(0)).handle(voidAsyncResult);
                return null;
              }
            })
        .when(voidFuture)
        .onComplete(any());

    utility
        .testInsert()
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                owner = getOwner();
                consumer = getConsumer();
                authHandler =
                    AuthHandler.create(api, authenticationService, client, postgresService);
                assertNotNull(authHandler);
                LOG.info("Set up the environment for testing successfully");
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Failed to set up");
              }
            });
  }

  public User getOwner() {
    JsonObject jsonObject =
        new JsonObject()
            .put("userId", utility.getOwnerId())
            .put("userRole", "provider")
            .put("emailId", utility.getOwnerEmailId())
            .put("firstName", utility.getOwnerFirstName())
            .put("resourceServerUrl", "rs.iudx.io")
            .put("lastName", utility.getOwnerLastName());
    return new User(jsonObject);
  }

  public User getConsumer() {
    JsonObject jsonObject =
        new JsonObject()
            .put("userId", utility.getConsumerId())
            .put("userRole", "consumer")
            .put("resourceServerUrl", "rs.iudx.io")
            .put("emailId", utility.getConsumerEmailId())
            .put("firstName", utility.getConsumerFirstName())
            .put("lastName", utility.getConsumerLastName());
    return new User(jsonObject);
  }

  @Test
  @DisplayName("Test handle method : Success")
  public void testHandleSuccess(VertxTestContext vertxTestContext) {
    authHandler.handle(routingContext);
    verify(routingContext, times(1)).next();
    verify(voidAsyncResult, times(1)).succeeded();
    assertTrue(voidAsyncResult.succeeded());
    verify(authenticationService, times(1)).tokenIntrospectForVerify(any());
    verify(httpServerRequest, times(1)).path();
    verify(httpServerRequest, times(1)).method();
    verify(routingContext, times(2)).request();
    verify(httpServerRequest, times(1)).headers();
    vertxTestContext.completeNow();
  }

  @ParameterizedTest
  @ValueSource(strings = {"User information is invalid", "Uh oh, something failed!"})
  @DisplayName("Test handle method with failed tokenIntrospectForVerify : Failure")
  public void testHandleWhenTokenIntrospect4VerifyFailure(
      String failureMessage, VertxTestContext vertxTestContext) {
    when(voidAsyncResult.succeeded()).thenReturn(false);
    when(voidAsyncResult.failed()).thenReturn(true);
    when(voidAsyncResult.cause()).thenReturn(throwable);
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);

    when(throwable.getMessage()).thenReturn(failureMessage);
    authHandler.handle(routingContext);

    verify(voidAsyncResult, times(1)).succeeded();
    assertFalse(voidAsyncResult.succeeded());
    assertTrue(voidAsyncResult.failed());
    assertEquals(failureMessage, throwable.getMessage());
    verify(authenticationService, times(1)).tokenIntrospectForVerify(any());
    verify(httpServerRequest, times(1)).path();
    verify(httpServerRequest, times(1)).method();
    verify(routingContext, times(2)).request();
    verify(routingContext, times(1)).response();
    verify(httpServerRequest, times(1)).headers();
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).setStatusCode(anyInt());
    verify(httpServerResponse, times(1)).end(anyString());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test handle method when token is invalid : Failure")
  public void testHandleWithInvalidToken(VertxTestContext vertxTestContext) {
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.end(anyString())).thenReturn(voidFuture);
    when(multiMapMock.get(anyString())).thenReturn("dummyToken");

    authHandler.handle(routingContext);
    verify(httpServerResponse, times(1)).putHeader(anyString(), anyString());
    verify(httpServerResponse, times(1)).setStatusCode(anyInt());
    verify(httpServerResponse, times(1)).end(anyString());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test handle with notification endpoint : Success")
  public void testHandleMethodForOtherEndpoints(VertxTestContext vertxTestContext) {
    JsonObject jsonObject =
        new JsonObject()
            .put("userId", utility.getConsumerId())
            .put(ROLE, "consumer")
            .put(AUD, "someDummyValue");

    when(httpServerRequest.path()).thenReturn(api.getRequestPoliciesUrl());
    when(authenticationService.tokenIntrospect(any()))
        .thenReturn(Future.succeededFuture(jsonObject));

    authHandler.handle(routingContext);

    verify(routingContext, times(2)).request();

    verify(authenticationService, times(1)).tokenIntrospect(any());

    vertxTestContext.completeNow();
  }

 /* @Test
  @DisplayName("Test getUserInfo method when the user is not present in DB: Success")
  public void testGetUserInfoSuccess(VertxTestContext vertxTestContext) {
    UUID anotherConsumer = Utility.generateRandomUuid();
    String emailId = Utility.generateRandomEmailId();
    String firstName = Utility.generateRandomString();
    String lastName = Utility.generateRandomString();
    String getUserInDbQuery = "SELECT * FROM user_table WHERE _id = $1::uuid";
    Tuple tuple = Tuple.of(anotherConsumer);
    JsonObject jsonObject =
        new JsonObject()
            .put("userId", anotherConsumer)
            .put(ROLE, "consumer")
            .put("userRole", "consumer")
            .put("resourceServerUrl", "rs.iudx.io")
            .put("emailId", emailId)
            .put("firstName", firstName)
            .put("lastName", lastName)
            .put(AUD, "someDummyValue");
    User consumer = new User(jsonObject);

    when(client.fetchUserInfo(any())).thenReturn(Future.succeededFuture(consumer));
    when(httpServerRequest.path()).thenReturn(api.getRequestPoliciesUrl());
    when(authenticationService.tokenIntrospect(any()))
        .thenReturn(Future.succeededFuture(jsonObject));

    authHandler.handle(routingContext);

    verify(routingContext, times(2)).request();
    verify(authenticationService, times(1)).tokenIntrospect(any());
    utility
        .executeQuery(tuple, getUserInDbQuery)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOG.info(handler.result().encodePrettily());
                JsonObject expectedResult =
                    handler.result().getJsonArray("response").getJsonObject(0);
                assertEquals(anotherConsumer.toString(), expectedResult.getString("_id"));
                assertEquals(emailId, expectedResult.getString("email_id"));
                assertEquals(firstName, expectedResult.getString("first_name"));
                assertEquals(lastName, expectedResult.getString("last_name"));
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Failed to fetch inserted user from the DB");
              }
            });
  }*/

  @Test
  @DisplayName("Test getUserInfo when authClient fails to fetch user details : Failure")
  public void testGetUserInfoWithAuthFailure(VertxTestContext vertxTestContext) {
    UUID anotherConsumer = Utility.generateRandomUuid();
    String emailId = Utility.generateRandomEmailId();
    String firstName = Utility.generateRandomString();
    String lastName = Utility.generateRandomString();
    JsonObject jsonObject =
        new JsonObject()
            .put("userId", anotherConsumer)
            .put(ROLE, "consumer")
            .put("userRole", "consumer")
            .put("resourceServerUrl", "rs.iudx.io")
            .put("emailId", emailId)
            .put("firstName", firstName)
            .put("lastName", lastName)
            .put(AUD, "someDummyValue");

    lenient()
        .when(client.fetchUserInfo(any()))
        .thenReturn(Future.failedFuture("Something went wrong..."));
    when(httpServerRequest.path()).thenReturn(api.getRequestPoliciesUrl());
    when(authenticationService.tokenIntrospect(any()))
        .thenReturn(Future.succeededFuture(jsonObject));
    lenient().when(routingContext.response()).thenReturn(httpServerResponse);
    authHandler.handle(routingContext);

    verify(routingContext, times(2)).request();
    verify(authenticationService, times(1)).tokenIntrospect(any());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test handle method with invalid endpoint : Failure")
  public void testHandleWithInvalidApi(VertxTestContext vertxTestContext) throws NullPointerException{
    when(httpServerRequest.path()).thenReturn("/some/api");
    assertThrows(NullPointerException.class, () -> authHandler.handle(routingContext));
    vertxTestContext.completeNow();
  }


}
