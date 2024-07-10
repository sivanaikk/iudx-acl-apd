package iudx.apd.acl.server.notification;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import iudx.apd.acl.server.Utility;
import iudx.apd.acl.server.apiserver.util.Role;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.policy.PostgresService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestGetNotifications {
  private static final Logger LOG = LoggerFactory.getLogger(TestGetNotifications.class);
  static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11");
  private static Utility utility;
  private static User owner;
  private static User consumer;
  private static GetNotification getNotification;

  @BeforeAll
  public static void setUp(VertxTestContext vertxTestContext) {
    utility = new Utility();
    container.start();
    PostgresService pgService = utility.setUp(container);

    utility
        .testInsert()
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                owner = getOwner();
                consumer = getConsumer();
                getNotification = new GetNotification(pgService);
                assertNotNull(getNotification);
                LOG.info("Set up the environment for testing successfully");
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Failed to set up");
              }
            });
  }

  public static User getOwner() {
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

  public static User getConsumer() {
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
  @DisplayName("Test GET Notification of consumer : Success")
  public void testGetNotificationSuccess(VertxTestContext vertxTestContext) {
    getNotification
        .initiateGetNotifications(consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject result =
                    handler.result().getJsonObject(RESULT).getJsonArray(RESULT).getJsonObject(0);
                assertEquals(utility.getRequestId().toString(), result.getString("requestId"));
                assertEquals(utility.getResourceId().toString(), result.getString("itemId"));
                assertEquals(utility.getRequestStatus(), result.getString("status"));
                assertEquals(utility.getConstraints().encode(), result.getString("constraints"));
                assertEquals(
                    utility.getConsumerId().toString(),
                    result.getJsonObject("consumer").getString("id"));

                assertEquals(
                    utility.getConsumerFirstName(),
                    result.getJsonObject("consumer").getJsonObject("name").getString("firstName"));
                assertEquals(
                    utility.getConsumerLastName(),
                    result.getJsonObject("consumer").getJsonObject("name").getString("lastName"));
                assertEquals(
                    utility.getConsumerEmailId(),
                    result.getJsonObject("consumer").getString("email"));
                assertEquals("rs.iudx.io", result.getString("resourceServerUrl"));

                assertEquals(
                    utility.getOwnerId().toString(),
                    result.getJsonObject("provider").getString("id"));
                assertEquals(
                    utility.getOwnerFirstName(),
                    result.getJsonObject("provider").getJsonObject("name").getString("firstName"));
                assertEquals(
                    utility.getOwnerLastName(),
                    result.getJsonObject("provider").getJsonObject("name").getString("lastName"));
                assertEquals(
                    utility.getOwnerEmailId(), result.getJsonObject("provider").getString("email"));
                assertEquals(new JsonObject(), result.getJsonObject("additionalInfo"));
                assertEquals(new JsonObject(), result.getJsonObject("constraints"));

                assertEquals(
                    HttpStatusCode.SUCCESS.getValue(),
                    handler.result().getInteger(STATUS_CODE).intValue());
                assertEquals(
                    ResponseUrn.SUCCESS_URN.getUrn(),
                    handler.result().getJsonObject(RESULT).getString(TYPE));
                assertEquals(
                    ResponseUrn.SUCCESS_URN.getMessage(),
                    handler.result().getJsonObject(RESULT).getString(TITLE));

                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Failed");
              }
            });
  }

  @Test
  @DisplayName("Test GET Notification for Provider : Success")
  public void testGetNotification4ProviderSuccess(VertxTestContext vertxTestContext) {
    getNotification
        .initiateGetNotifications(owner)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject result =
                    handler.result().getJsonObject(RESULT).getJsonArray(RESULT).getJsonObject(0);
                assertEquals(utility.getRequestId().toString(), result.getString("requestId"));
                assertEquals(utility.getResourceId().toString(), result.getString("itemId"));
                assertEquals(utility.getRequestStatus(), result.getString("status"));
                assertEquals(utility.getConstraints().encode(), result.getString("constraints"));
                assertEquals(
                    utility.getConsumerId().toString(),
                    result.getJsonObject("consumer").getString("id"));

                assertEquals(
                    utility.getConsumerFirstName(),
                    result.getJsonObject("consumer").getJsonObject("name").getString("firstName"));
                assertEquals(
                    utility.getConsumerLastName(),
                    result.getJsonObject("consumer").getJsonObject("name").getString("lastName"));
                assertEquals(
                    utility.getConsumerEmailId(),
                    result.getJsonObject("consumer").getString("email"));
                  assertEquals("rs.iudx.io", result.getString("resourceServerUrl"));

                assertEquals(
                    utility.getOwnerId().toString(),
                    result.getJsonObject("provider").getString("id"));
                assertEquals(
                    utility.getOwnerFirstName(),
                    result.getJsonObject("provider").getJsonObject("name").getString("firstName"));
                assertEquals(
                    utility.getOwnerLastName(),
                    result.getJsonObject("provider").getJsonObject("name").getString("lastName"));
                assertEquals(
                    utility.getOwnerEmailId(), result.getJsonObject("provider").getString("email"));

                assertEquals(
                    HttpStatusCode.SUCCESS.getValue(),
                    handler.result().getInteger(STATUS_CODE).intValue());
                assertEquals(
                    ResponseUrn.SUCCESS_URN.getUrn(),
                    handler.result().getJsonObject(RESULT).getString(TYPE));
                assertEquals(
                    ResponseUrn.SUCCESS_URN.getMessage(),
                    handler.result().getJsonObject(RESULT).getString(TITLE));

                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Failed");
              }
            });
  }

  @Test
  @DisplayName("Test executeGetNotification method with no requests")
  public void testExecuteGetNotification(VertxTestContext vertxTestContext) {
    JsonObject jsonObject =
        new JsonObject()
            .put("userId", UUID.randomUUID())
            .put("userRole", "consumer")
            .put("resourceServerUrl", "rs.iudx.io")
            .put("emailId", utility.getConsumerEmailId())
            .put("firstName", utility.getConsumerFirstName())
            .put("lastName", utility.getConsumerLastName());
    getNotification
        .initiateGetNotifications(new User(jsonObject))
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded for non-existing user Id");
              } else {

                JsonObject failureMessage =
                    new JsonObject()
                        .put(TYPE, HttpStatusCode.NOT_FOUND.getValue())
                        .put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                        .put(DETAIL, "Access request not found, for the server : rs.iudx.io");
                assertEquals(failureMessage.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test executeGetNotification method when DB connection failed")
  public void testExecuteGetNotificationFailure(VertxTestContext vertxTestContext) {
    PostgresService pgService = mock(PostgresService.class);
    PgPool pgPool = mock(PgPool.class);
    when(pgPool.withConnection(any())).thenReturn(Future.failedFuture("Some failure message"));
    when(pgService.getPool()).thenReturn(pgPool);
    GetNotification getNotification = new GetNotification(pgService);
    getNotification
        .initiateGetNotifications(consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded for failed connection");
              } else {
                JsonObject failureMessage =
                    new JsonObject()
                        .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                        .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                        .put(
                            DETAIL,
                            "Notifications could not be fetched, Failure while executing query");
                assertEquals(failureMessage.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test executeGetNotification method when Pgpool is null")
  public void testExecuteGetNotificationWithNullPgPool(VertxTestContext vertxTestContext) {
    PostgresService pgService = mock(PostgresService.class);
    when(pgService.getPool()).thenReturn(null);
    GetNotification getNotification = new GetNotification(pgService);
    assertThrows(
        NullPointerException.class, () -> getNotification.initiateGetNotifications(consumer));
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test getUserNotification with null user")
  public void testGetUserNotificationWithNullUser(VertxTestContext vertxTestContext) {

    assertThrows(
        NullPointerException.class,
        () -> getNotification.getUserNotification(null, "SELECT * FROM policy", Role.CONSUMER));
    vertxTestContext.completeNow();
  }
}
