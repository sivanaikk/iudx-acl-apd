package iudx.apd.acl.server.authorization;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import iudx.apd.acl.server.Utility;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.authentication.AuthClient;
import iudx.apd.acl.server.authentication.AuthHandler;
import iudx.apd.acl.server.authentication.AuthenticationService;
import iudx.apd.acl.server.common.Api;
import iudx.apd.acl.server.policy.PostgresService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestAuthClient {
    private static final Logger LOG = LoggerFactory.getLogger(TestAuthClient.class);
    static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11");
    private static User owner;
    private static User consumer;
    @Mock
    HttpServerRequest httpServerRequest;
    @Mock
    MultiMap multiMapMock;
    @Mock
    HttpMethod httpMethod;
    @Mock
    Future<JsonObject> future;
    @Mock Future<Void> voidFuture;
    @Mock Void aVoid;
    @Mock
    AsyncResult<Void> voidAsyncResult;
    @Mock Throwable throwable;
    @Mock
    HttpServerResponse httpServerResponse;
    @Mock
    PgPool pgPool;
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
}
