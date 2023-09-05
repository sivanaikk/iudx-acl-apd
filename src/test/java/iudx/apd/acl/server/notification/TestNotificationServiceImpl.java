package iudx.apd.acl.server.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.Utility;
import iudx.apd.acl.server.apiserver.util.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestNotificationServiceImpl {
  @Container static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11");
  Utility utility;
  User consumer;
  User provider;
  @Mock DeleteNotification deleteNotification;
  @Mock CreateNotification createNotification;
  @Mock UpdateNotification updateNotification;
  @Mock GetNotification getNotification;
  @Mock JsonObject request;
  @Mock Future<JsonObject> future;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Throwable throwable;
  JsonObject jsonObject;
  NotificationServiceImpl service;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {

    utility = new Utility();

    consumer = getConsumer();
    provider = getOwner();
    service =
        new NotificationServiceImpl(
            deleteNotification, updateNotification, getNotification, createNotification);
    jsonObject = new JsonObject().put("result", "someResponse");
    lenient().when(createNotification.initiateCreateNotification(any(), any())).thenReturn(future);
    lenient().when(asyncResult.succeeded()).thenReturn(true);
    lenient().when(asyncResult.result()).thenReturn(jsonObject);
    lenient()
        .doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(future)
        .onComplete(any(Handler.class));
    lenient().when(deleteNotification.initiateDeleteNotification(any(), any())).thenReturn(future);
    lenient().when(updateNotification.initiateUpdateNotification(any(), any())).thenReturn(future);
    lenient().when(getNotification.initiateGetNotifications(any())).thenReturn(future);
    vertxTestContext.completeNow();
  }

  public User getConsumer() {
    JsonObject jsonObject =
        new JsonObject()
            .put("userId", Utility.generateRandomString())
            .put("userRole", "consumer")
            .put("emailId", Utility.generateRandomEmailId())
            .put("firstName", Utility.generateRandomString())
            .put("lastName", Utility.generateRandomString());
    return new User(jsonObject);
  }

  public User getOwner() {
    JsonObject jsonObject =
        new JsonObject()
            .put("userId", Utility.generateRandomString())
            .put("userRole", "provider")
            .put("emailId", Utility.generateRandomEmailId())
            .put("firstName", Utility.generateRandomString())
            .put("lastName", Utility.generateRandomString());
    return new User(jsonObject);
  }

  @Test
  @DisplayName("Test createNotification : Success")
  public void testCreateNotificationSuccess(VertxTestContext vertxTestContext) {

    service
        .createNotification(request, consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(jsonObject, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause().getMessage());
              }
            });
  }

  @Test
  @DisplayName("Test deleteNotification : Success")
  public void testDeleteNotificationSuccess(VertxTestContext vertxTestContext) {

    service
        .deleteNotification(request, consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(jsonObject, handler.result());
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow(handler.cause().getMessage());
              }
            });
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test getNotification : Success")
  public void testGetNotificationSuccess(VertxTestContext vertxTestContext) {

    service
        .getNotification(consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(jsonObject, handler.result());
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow(handler.cause().getMessage());
              }
            });
  }

  @Test
  @DisplayName("Test updateNotification : Success")
  public void testUpdateNotificationSuccess(VertxTestContext vertxTestContext) {

    service
        .updateNotification(request, provider)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(jsonObject, handler.result());
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow(handler.cause().getMessage());
              }
            });
  }

  @Test
  @DisplayName("Test createNotification : Failure")
  public void testCreateNotificationFailure(VertxTestContext vertxTestContext) {
    callFailureMocks();

    service
        .createNotification(request, consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when creating notification failed");
              } else {
                assertEquals("Some Failure Message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test deleteNotification : Failure")
  public void testDeleteNotificationFailure(VertxTestContext vertxTestContext) {
    callFailureMocks();
    service
        .deleteNotification(request, consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when deleting notification failed");
              } else {
                assertEquals("Some Failure Message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test getNotification : Failure")
  public void testGetNotificationFailure(VertxTestContext vertxTestContext) {
    callFailureMocks();
    service
        .getNotification(consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when fetching notification failed");
              } else {
                assertEquals("Some Failure Message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test updateNotification : Failure")
  public void testUpdateNotificationFailure(VertxTestContext vertxTestContext) {
    callFailureMocks();
    service
        .updateNotification(request, provider)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when updating notification failed");

              } else {
                assertEquals("Some Failure Message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  private void callFailureMocks() {
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Some Failure Message");
  }
}
