package iudx.apd.acl.server.notification;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.ResponseUrn.POLICY_ALREADY_EXIST_URN;
import static iudx.apd.acl.server.notification.util.Constants.CREATE_NOTIFICATION_QUERY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.Utility;
import iudx.apd.acl.server.apiserver.util.ResourceObj;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.policy.CatalogueClient;
import iudx.apd.acl.server.policy.PostgresService;
import iudx.apd.acl.server.policy.util.ItemType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestCreateNotification {
  private static final Logger LOG = LoggerFactory.getLogger(TestUpdateNotifications.class);
  private static final String TYPE_RESOURCE = "RESOURCE";
  private static final String TYPE_RESOURCE_GROUP = "RESOURCE_GROUP";
  static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11");
  static PostgresService pgService;
  static CatalogueClient catClient;
  @Mock static Future<List<ResourceObj>> resourceInfo;
  private static Utility utility;
  private static CreateNotification createNotification;
  private static User owner;
  private static User consumer;
  private static JsonObject notification;
  private static UUID itemId;
  @Mock Throwable throwable;
  @Mock ResourceObj resourceObj;
  @Mock PostgresService postgresService;
  @Mock CatalogueClient catalogueClient;
  @Mock PgPool pool;

  @BeforeAll
  public static void setUp(VertxTestContext vertxTestContext) {
    container.start();
    utility = new Utility();
    pgService = utility.setUp(container);
    catClient = mock(CatalogueClient.class);
    utility
        .testInsert()
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                itemId = UUID.randomUUID();
                consumer = getConsumer();
                owner = getOwner();
                notification =
                    new JsonObject().put("itemId", itemId).put("itemType", TYPE_RESOURCE);
                assertNotNull(itemId);
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
            .put("lastName", utility.getOwnerLastName());
    return new User(jsonObject);
  }

  public static User getConsumer() {
    JsonObject jsonObject =
        new JsonObject()
            .put("userId", utility.getConsumerId())
            .put("userRole", "consumer")
            .put("emailId", utility.getConsumerEmailId())
            .put("firstName", utility.getConsumerFirstName())
            .put("lastName", utility.getConsumerLastName());
    return new User(jsonObject);
  }

  @Test
  @DisplayName("Test initiateCreateNotification : Success")
  public void testInitiateCreateNotification(VertxTestContext vertxTestContext) {
    catClient = mock(CatalogueClient.class);
    EmailNotification emailNotification = mock(EmailNotification.class);
    List<ResourceObj> resourceObjList = mock(List.class);

    when(emailNotification.sendEmail(any(User.class), any(User.class), anyString(), anyString()))
        .thenReturn(Future.succeededFuture(true));
    createNotification = new CreateNotification(pgService, catClient, emailNotification);
    when(catClient.fetchItems(any())).thenReturn(resourceInfo);

    AsyncResult<List<ResourceObj>> asyncResult = mock(AsyncResult.class);

    doAnswer(
            new Answer<AsyncResult<List<ResourceObj>>>() {
              @Override
              public AsyncResult<List<ResourceObj>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<List<ResourceObj>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(resourceInfo)
        .onComplete(any());
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(resourceObjList);
    when(resourceObjList.get(anyInt())).thenReturn(resourceObj);
    when(resourceObj.getResourceServerUrl()).thenReturn("rs.iudx.io");
    when(resourceObj.getResourceGroupId()).thenReturn(UUID.randomUUID());
    when(resourceObj.getProviderId()).thenReturn(utility.getOwnerId());
    when(resourceObj.getItemType()).thenReturn(ItemType.RESOURCE);
    createNotification
        .initiateCreateNotification(notification, consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(ResponseUrn.SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
                assertEquals(
                    ResponseUrn.SUCCESS_URN.getMessage(), handler.result().getString(TITLE));
                assertEquals("Request inserted successfully!", handler.result().getString(RESULT));
                utility
                    .executeQuery(Tuple.tuple(), "SELECT * FROM request")
                    .onComplete(
                        selectQueryHandler -> {
                          JsonObject response =
                              selectQueryHandler.result().getJsonArray("response").getJsonObject(1);
                          assertEquals(
                              utility.getOwnerId().toString(), response.getString("owner_id"));
                          assertEquals(itemId.toString(), response.getString("item_id"));
                          assertEquals(
                              utility.getConsumerId().toString(), response.getString("user_id"));
                          assertEquals("PENDING", response.getString("status"));
                          assertNull(response.getString("expiry_at"));
                          assertNull(response.getString("constraints"));
                          vertxTestContext.completeNow();
                        });

              } else {
                vertxTestContext.failNow("Failed : " + handler.cause().getMessage());
              }
            });
  }

  @Test
  @DisplayName("Test initiateCreateNotification method when policy is already created : Failure")
  public void testWithPolicyAlreadyCreated(VertxTestContext vertxTestContext) {
    catClient = mock(CatalogueClient.class);
    UUID resourceId = Utility.generateRandomUuid();
    Tuple resourceInsertionTuple =
        Tuple.of(
            resourceId,
            utility.getOwnerId(),
            null,
            "rsUrl",
            LocalDateTime.now(),
            LocalDateTime.of(2023, 12, 10, 3, 20, 10, 9),ItemType.RESOURCE);
    LOG.info(
        " user_emailid : "
            + consumer.getEmailId()
            + " | item_id: "
            + resourceId);
    Tuple policyInsertionTuple =
        Tuple.of(
            Utility.generateRandomUuid(),
            consumer.getEmailId(),
            resourceId,
            utility.getOwnerId(),
            utility.getStatus(),
            LocalDateTime.of(2025, 12, 10, 3, 20, 20, 29),
            new JsonObject(),
            LocalDateTime.now(),
            LocalDateTime.of(2023, 12, 10, 3, 20, 10, 9));

    EmailNotification emailNotification = mock(EmailNotification.class);
    List<ResourceObj> resourceObjList = mock(List.class);
    when(catClient.fetchItems(any())).thenReturn(resourceInfo);

    AsyncResult<List<ResourceObj>> asyncResult = mock(AsyncResult.class);

    doAnswer(
            new Answer<AsyncResult<List<ResourceObj>>>() {
              @Override
              public AsyncResult<List<ResourceObj>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<List<ResourceObj>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(resourceInfo)
        .onComplete(any());

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(resourceObjList);
    when(resourceObjList.get(anyInt())).thenReturn(resourceObj);
    when(resourceObj.getResourceGroupId()).thenReturn(null);
    when(resourceObj.getProviderId()).thenReturn(utility.getOwnerId());
    when(resourceObj.getItemType()).thenReturn(ItemType.RESOURCE);
    when(resourceObj.getResourceServerUrl()).thenReturn("rsUrl");
    JsonObject notification =
        new JsonObject().put("itemId", resourceId);

    utility
        .executeQuery(resourceInsertionTuple, Utility.INSERT_INTO_RESOURCE_ENTITY_TABLE)
        .onComplete(
            handler -> {
              LOG.info("Inserted in to rs");
              if (handler.succeeded()) {
                utility
                    .executeQuery(policyInsertionTuple, Utility.INSERT_INTO_POLICY_TABLE)
                    .onComplete(
                        policyInsertionHandler -> {
                          LOG.info("Inserted in to policy");

                          if (policyInsertionHandler.succeeded()) {
                            createNotification =
                                new CreateNotification(pgService, catClient, emailNotification);
                            createNotification
                                .initiateCreateNotification(notification, consumer)
                                .onComplete(
                                    createNotificationHandler -> {
                                      if (createNotificationHandler.succeeded()) {
                                        vertxTestContext.failNow(
                                            "Succeeded when the policy was previously for the consumer");

                                      } else {
                                        LOG.info(createNotificationHandler.cause().getMessage());
                                        JsonObject failureMessage =
                                            new JsonObject()
                                                .put(TYPE, 409)
                                                .put(TITLE, POLICY_ALREADY_EXIST_URN.getUrn())
                                                .put(
                                                    DETAIL,
                                                    "Request could not be created, as a policy is already present");
                                        assertEquals(
                                            failureMessage.encode(),
                                            createNotificationHandler.cause().getMessage());
                                        vertxTestContext.completeNow();
                                      }
                                    });
                          } else {
                            vertxTestContext.failNow("Could not create policy");
                          }
                        });

              } else {
                vertxTestContext.failNow("Could not insert resource in the table");
              }
            });
  }

  @Test
  @DisplayName("Test initiateCreateNotification method when request is already created : Failure")
  public void testWithNotificationAlreadyCreated(VertxTestContext vertxTestContext) {
    catClient = mock(CatalogueClient.class);
    EmailNotification emailNotification = mock(EmailNotification.class);
    List<ResourceObj> resourceObjList = mock(List.class);
    UUID itemIdValue = UUID.randomUUID();

    createNotification = new CreateNotification(pgService, catClient, emailNotification);
    when(catClient.fetchItems(any())).thenReturn(resourceInfo);
    JsonObject notification =
        new JsonObject().put("itemId", itemIdValue);
    AsyncResult<List<ResourceObj>> asyncResult = mock(AsyncResult.class);

    doAnswer(
            new Answer<AsyncResult<List<ResourceObj>>>() {
              @Override
              public AsyncResult<List<ResourceObj>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<List<ResourceObj>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(resourceInfo)
        .onComplete(any());
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(resourceObjList);
    when(resourceObjList.get(anyInt())).thenReturn(resourceObj);
    when(resourceObj.getResourceGroupId()).thenReturn(null);
    when(resourceObj.getProviderId()).thenReturn(utility.getOwnerId());
    when(resourceObj.getItemType()).thenReturn(ItemType.RESOURCE);
    when(resourceObj.getResourceServerUrl()).thenReturn("rsUrl");
    createNotification
        .initiateCreateNotification(notification, consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(ResponseUrn.SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
                assertEquals(
                    ResponseUrn.SUCCESS_URN.getMessage(), handler.result().getString(TITLE));
                assertEquals("Request inserted successfully!", handler.result().getString(RESULT));
                createNotification
                    .initiateCreateNotification(notification, consumer)
                    .onComplete(
                        createNotificationHandler -> {
                          if (createNotificationHandler.succeeded()) {
                            vertxTestContext.failNow(
                                "Succeeded when notification is already created");
                          } else {
                            JsonObject failureMessage =
                                new JsonObject()
                                    .put(TYPE, HttpStatusCode.CONFLICT.getValue())
                                    .put(TITLE, POLICY_ALREADY_EXIST_URN.getUrn())
                                    .put(
                                        DETAIL,
                                        "Request could not be created, as a request for the given resource has been previously made");
                            assertEquals(
                                failureMessage.encode(),
                                createNotificationHandler.cause().getMessage());
                            vertxTestContext.completeNow();
                          }
                        });

              } else {
                vertxTestContext.failNow("Failed : " + handler.cause().getMessage());
              }
            });
  }

  @Test
  @DisplayName(
      "Test initiateCreateNotification when the catalogue returns failed response: Failure")
  public void testWithFailedResponseFromCat(VertxTestContext vertxTestContext) {
    catClient = mock(CatalogueClient.class);
    List<ResourceObj> resourceObjList = mock(List.class);
    EmailNotification emailNotification = mock(EmailNotification.class);

    createNotification = new CreateNotification(pgService, catClient, emailNotification);
    when(catClient.fetchItems(any())).thenReturn(resourceInfo);

    AsyncResult<List<ResourceObj>> asyncResult = mock(AsyncResult.class);

    doAnswer(
            new Answer<AsyncResult<List<ResourceObj>>>() {
              @Override
              public AsyncResult<List<ResourceObj>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<List<ResourceObj>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(resourceInfo)
        .onComplete(any());
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Id/Ids does not present in CAT");

    createNotification
        .initiateCreateNotification(notification, consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow(
                    "Succeeded when the response it failed to get response from catalogue");
              } else {
                JsonObject failureMessage =
                    new JsonObject()
                        .put(TYPE, 404)
                        .put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                        .put(
                            DETAIL,
                            "Request could not be created, as resource was not found");
                assertEquals(failureMessage.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName(
      "Test initiateCreateNotification when something goes wrong while fetching items from CAT: Failure")
  public void testWhileFetchingItemFromCatFailed(VertxTestContext vertxTestContext) {
    catClient = mock(CatalogueClient.class);
    EmailNotification emailNotification = mock(EmailNotification.class);

    createNotification = new CreateNotification(pgService, catClient, emailNotification);
    when(catClient.fetchItems(any())).thenReturn(resourceInfo);

    AsyncResult<List<ResourceObj>> asyncResult = mock(AsyncResult.class);

    doAnswer(
            new Answer<AsyncResult<List<ResourceObj>>>() {
              @Override
              public AsyncResult<List<ResourceObj>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<List<ResourceObj>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(resourceInfo)
        .onComplete(any());
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Some dummy error");

    createNotification
        .initiateCreateNotification(notification, consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow(
                    "Succeeded when the response it failed to get response from catalogue");
              } else {
                JsonObject failureMessage =
                    new JsonObject()
                        .put(TYPE, 500)
                        .put(TITLE, ResponseUrn.INTERNAL_SERVER_ERROR.getUrn())
                        .put(DETAIL, "Request could not be created");
                assertEquals(failureMessage.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test createNotification method when there is a database failure : Failure")
  public void testCreateNotificationWithDbFailure(VertxTestContext vertxTestContext) {
    UUID resourceId = UUID.randomUUID();
    EmailNotification emailNotification = mock(EmailNotification.class);

    when(postgresService.getPool()).thenReturn(pool);
    when(pool.withConnection(any())).thenReturn(Future.failedFuture("Something went wrong :("));
    CreateNotification createNotification =
        new CreateNotification(postgresService, catalogueClient, emailNotification);

    createNotification
        .createNotification(
            CREATE_NOTIFICATION_QUERY,
            resourceId,
            consumer,
            utility.getOwnerId())
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when the connection with DB failed");
              } else {
                JsonObject failureMessage = new JsonObject(handler.cause().getMessage());
                assertEquals(500, failureMessage.getInteger(TYPE));
                assertEquals(ResponseUrn.DB_ERROR_URN.getUrn(), failureMessage.getString(TITLE));
                assertEquals("Failure while executing query", failureMessage.getString(DETAIL));
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName(
      "Test createNotification method when there is empty response from database : Failure")
  public void testCreateNotificationWithEmptyResponse(VertxTestContext vertxTestContext) {
    EmailNotification emailNotification = mock(EmailNotification.class);

    UUID resourceId = UUID.randomUUID();
    when(postgresService.getPool()).thenReturn(pool);
    when(pool.withConnection(any())).thenReturn(Future.succeededFuture(List.of()));
    CreateNotification createNotification =
        new CreateNotification(postgresService, catalogueClient, emailNotification);

    createNotification
        .createNotification(
            CREATE_NOTIFICATION_QUERY,
            resourceId,
          consumer,
            utility.getOwnerId())
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when the connection with DB failed");
              } else {
                JsonObject failureMessage = new JsonObject(handler.cause().getMessage());
                assertEquals(500, failureMessage.getInteger(TYPE));
                assertEquals(
                    ResponseUrn.INTERNAL_SERVER_ERROR.getUrn(), failureMessage.getString(TITLE));
                assertEquals("Request could not be created", failureMessage.getString(DETAIL));
                vertxTestContext.completeNow();
              }
            });
  }
}
