package iudx.apd.acl.server.authorization;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import iudx.apd.acl.server.Utility;
import iudx.apd.acl.server.apiserver.util.Role;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.authentication.AuthClient;
import iudx.apd.acl.server.authentication.AuthHandler;
import iudx.apd.acl.server.authentication.AuthenticationService;
import iudx.apd.acl.server.common.Api;
import iudx.apd.acl.server.policy.PostgresService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.Buffer;

import static iudx.apd.acl.server.apiserver.util.Constants.RESULT;
import static iudx.apd.acl.server.authentication.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestAuthClient {
  private static final Logger LOG = LoggerFactory.getLogger(TestAuthClient.class);
  static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11");
  private static User owner;
  private static User consumer;
  @Mock HttpRequest httpServerRequest;
  @Mock MultiMap multiMapMock;
  @Mock HttpMethod httpMethod;
  @Mock Future<HttpResponse<Buffer>> future;
  @Mock Future<Void> voidFuture;
  @Mock Void aVoid;
  @Mock AsyncResult<Void> voidAsyncResult;
  @Mock Throwable throwable;
  @Mock HttpResponse httpServerResponse;
  @Mock PgPool pgPool;
  @Mock HttpRequest<io.vertx.core.buffer.Buffer> bufferHttpRequest;
  @Mock HttpResponse<io.vertx.core.buffer.Buffer> bufferHttpResponse;
  @Mock Future<HttpResponse<io.vertx.core.buffer.Buffer>> httpResponseFuture;

  WebClient webClient;
  @Mock AsyncResult<HttpResponse<Buffer>> asyncResult;
  private Utility utility;
  private AuthHandler authHandler;
  private JsonObject config;
  private Api api;
  private AuthenticationService authenticationService;
  private AuthClient client;
  private PostgresService postgresService;
  private RoutingContext routingContext;
  private String emailId;
  private String firstName;
  private String lastName;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    utility = new Utility();
    container.start();
    webClient = mock(WebClient.class);
    postgresService = utility.setUp(container);

    config =
        new JsonObject()
            .put("authHost", "dummyAuthHost")
            .put("authPort", 443)
            .put("dxAuthBasePath", "dummy/base/path")
            .put("type", "urn:dx:as:Success")
            .put("clientId", Utility.generateRandomUuid())
            .put("clientSecret", Utility.generateRandomUuid());

    lenient().when(webClient.get(anyInt(),anyString(), anyString())).thenReturn(httpServerRequest);
    lenient()
        .when(httpServerRequest.putHeader(anyString(), anyString()))
        .thenReturn(httpServerRequest);
    lenient()
        .when(httpServerRequest.addQueryParam(anyString(), anyString()))
        .thenReturn(httpServerRequest);
    lenient().when(httpServerRequest.send()).thenReturn(future);
    lenient()
        .doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg2)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg2.getArgument(0))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(future)
        .onComplete(any());
    lenient().when(asyncResult.result()).thenReturn(httpServerResponse);
    lenient().when(httpServerResponse.bodyAsJsonObject()).thenReturn(config);
    lenient().when(asyncResult.succeeded()).thenReturn(true);

    utility
        .testInsert()
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                emailId = utility.getConsumerEmailId();
                firstName = utility.getConsumerFirstName();
                lastName = utility.getConsumerLastName();

                owner = getOwner();
                consumer = getConsumer();
                config.put(USER_ID, utility.getConsumerId());
                config.put(ROLE, Role.CONSUMER.getRole());
                config.put(AUD, "rs.iudx.io");
                config.put(IS_DELEGATE, false);
                JsonObject results =
                    new JsonObject()
                        .put("email", emailId)
                        .put(
                            "name",
                            new JsonObject().put("firstName", firstName).put("lastName", lastName));
                config.put(RESULT, results);
                client = new AuthClient(config, webClient);

                assertNotNull(client);
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
  @DisplayName("Test fetchUserInfo method : Success")
  public void testFetchUserInfoSuccess(VertxTestContext vertxTestContext) {

    client
        .fetchUserInfo(config)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {

                assertEquals(consumer, handler.result());
                verify(webClient, times(1)).get(anyInt(), anyString(), anyString());
                verify(httpServerRequest, times(2)).putHeader(anyString(), anyString());
                verify(httpServerRequest, times(3)).addQueryParam(anyString(), anyString());
                verify(httpServerRequest, times(1)).send();
                assertTrue(asyncResult.succeeded());
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Failed");
              }
            });
  }

  @Test
  @DisplayName("Test fetchUserInfo method when fetch from Auth fails : Failure")
  public void testFetchUserInfoWithAuthFailure(VertxTestContext vertxTestContext) {
    lenient().when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Something went wrong : (");

    client
        .fetchUserInfo(config)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when fetch user info from Auth failed");
              } else {
                assertEquals("Internal Server Error", handler.cause().getMessage());
                verify(webClient, times(1)).get(anyInt(),anyString(), anyString());
                verify(httpServerRequest, times(2)).putHeader(anyString(), anyString());
                verify(httpServerRequest, times(3)).addQueryParam(anyString(), anyString());
                verify(httpServerRequest, times(1)).send();
                assertFalse(asyncResult.failed());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test fetchUserInfo method when the user is not found in auth : Failure")
  public void testFetchUserInfoWithNoUserPresentInAuth(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = config;
    jsonObject.put("type", "some:other:urn:Dummy");
    AuthClient client = new AuthClient(config, webClient);

    client
        .fetchUserInfo(config)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when no user was found in AAA server");
              } else {
                assertEquals("User not present in Auth.", handler.cause().getMessage());
                verify(webClient, times(1)).get(anyInt(), anyString(), anyString());
                verify(httpServerRequest, times(2)).putHeader(anyString(), anyString());
                verify(httpServerRequest, times(3)).addQueryParam(anyString(), anyString());
                verify(httpServerRequest, times(1)).send();
                assertFalse(asyncResult.failed());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName(
      "Test fetchUserInfo method when there response from auth contains null value : Failure")
  public void testFetchUserWithNullResponseFromAuth(VertxTestContext vertxTestContext) {

    Utility utility = new Utility();
    container.start();
    utility.setUp(container);
    WebClient webClient = mock(WebClient.class);
    JsonObject configJson =
        new JsonObject()
            .put("authHost", "dummyAuthHost")
            .put("authPort", 443)
            .put("dxAuthBasePath", "dummy/base/path")
            .put("type", "urn:dx:as:Success")
            .put("clientId", Utility.generateRandomUuid())
            .put("clientSecret", Utility.generateRandomUuid());

    lenient().when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(httpServerRequest);
    lenient()
        .when(httpServerRequest.putHeader(anyString(), anyString()))
        .thenReturn(httpServerRequest);
    lenient()
        .when(httpServerRequest.addQueryParam(anyString(), anyString()))
        .thenReturn(httpServerRequest);
    lenient().when(httpServerRequest.send()).thenReturn(future);
    lenient()
        .doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg2)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg2.getArgument(0))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(future)
        .onComplete(any());
    lenient().when(asyncResult.result()).thenReturn(httpServerResponse);
    lenient().when(httpServerResponse.bodyAsJsonObject()).thenReturn(configJson);
    lenient().when(asyncResult.succeeded()).thenReturn(true);

    utility
        .testInsert()
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                String emailId = utility.getConsumerEmailId();
                String firstName = utility.getConsumerFirstName();
                User consumer = getConsumer();
                configJson.put(USER_ID, utility.getConsumerId());
                configJson.put(ROLE, Role.CONSUMER.getRole());
                configJson.put(AUD, "rs.iudx.io");
                configJson.put(IS_DELEGATE, false);
                JsonObject results =
                    new JsonObject()
                        .put("email", emailId)
                        .put(
                            "name",
                            new JsonObject().put("firstName", firstName).put("lastName", null));
                configJson.put(RESULT, results);
                AuthClient client = new AuthClient(configJson, webClient);
                assertNotNull(client);
                LOG.info("Set up the environment for testing successfully");

                client
                    .fetchUserInfo(configJson)
                    .onComplete(
                        authHandler -> {
                          if (authHandler.succeeded()) {
                            vertxTestContext.failNow(
                                "Succeeded when a value from Auth server response is null");
                          } else {
                            assertEquals(
                                "User information is invalid", authHandler.cause().getMessage());
                            verify(webClient, times(1)).get(anyInt(), anyString(), anyString());
                            verify(httpServerRequest, times(2)).putHeader(anyString(), anyString());
                            verify(httpServerRequest, times(3))
                                .addQueryParam(anyString(), anyString());
                            verify(httpServerRequest, times(1)).send();
                            assertFalse(asyncResult.failed());
                            vertxTestContext.completeNow();
                          }
                        });
              } else {
                vertxTestContext.failNow("Failed to set up");
              }
            });
  }
}
