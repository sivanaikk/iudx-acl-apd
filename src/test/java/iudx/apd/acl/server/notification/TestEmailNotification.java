package iudx.apd.acl.server.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.Utility;
import iudx.apd.acl.server.apiserver.util.User;
import java.io.IOException;
import java.util.List;
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
public class TestEmailNotification {
  @Container static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11");
  EmailNotification emailNotification;
  JsonObject config;
  @Mock GetDelegateEmailIds getDelegateEmailIds;
  @Mock Future<JsonArray> jsonArrayFuture;
  @Mock Future<MailResult> mailResultFuture;
  @Mock AsyncResult<JsonArray> asyncResult;
  @Mock AsyncResult<MailResult> mailResultAsyncResult;
  @Mock JsonArray jsonArray;
  @Mock MailResult mailResult;
  @Mock Throwable throwable;
  MailClient mailClient;
  User consumer;
  User provider;
  Utility utility;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) throws IOException {
    container.start();
    utility = new Utility();
    utility.setUp(container);
    Vertx vertx = Vertx.vertx();
    config =
        new JsonObject()
            .put("emailHostName", "someHost")
            .put("emailHostName", "http://localhost")
            .put("emailPort", 1245)
            .put("emailUserName", "someName")
            .put("emailPassword", "dummyPassword")
            .put("emailSender", "dummySender")
            .put(
                "emailSupport",
                List.of("dummySupport@outlook.com", "dummySupportEmailId@rediffmail.com"))
            .put("publisherPanelUrl", "https://somePanelUrl.com")
            .put("notifyByEmail", true)
            .put("senderName", "someSenderName");
    consumer = getConsumer();
    provider = getOwner();
    lenient()
        .when(getDelegateEmailIds.getEmails(anyString(), anyString(), anyString()))
        .thenReturn(jsonArrayFuture);
    lenient()
        .doAnswer(
            new Answer<AsyncResult<JsonArray>>() {
              @Override
              public AsyncResult<JsonArray> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonArray>>) arg2.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonArrayFuture)
        .onComplete(any(Handler.class));
    lenient().when(asyncResult.succeeded()).thenReturn(true);
    lenient().when(asyncResult.result()).thenReturn(jsonArray);
    lenient()
        .when(jsonArray.getList())
        .thenReturn(List.of(Utility.generateRandomEmailId(), Utility.generateRandomEmailId()));

    emailNotification = new EmailNotification(vertx, config, getDelegateEmailIds);
    emailNotification.mailClient = mock(MailClient.class);
    mailClient = emailNotification.mailClient;
    lenient().when(mailClient.sendMail(any(MailMessage.class))).thenReturn(mailResultFuture);
    lenient()
        .doAnswer(
            new Answer<AsyncResult<MailResult>>() {
              @Override
              public AsyncResult<MailResult> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<MailResult>>) arg2.getArgument(0))
                    .handle(mailResultAsyncResult);
                return null;
              }
            })
        .when(mailResultFuture)
        .onComplete(any(Handler.class));
    lenient().when(mailResultAsyncResult.succeeded()).thenReturn(true);
    lenient().when(mailResultAsyncResult.result()).thenReturn(mailResult);
    vertxTestContext.completeNow();
  }

  public User getConsumer() {
    JsonObject jsonObject =
        new JsonObject()
            .put("userId", utility.getConsumerId())
            .put("userRole", "consumer")
            .put("emailId", utility.getConsumerEmailId())
            .put("firstName", utility.getConsumerFirstName())
            .put("lastName", utility.getConsumerLastName());
    return new User(jsonObject);
  }

  public User getOwner() {
    JsonObject jsonObject =
        new JsonObject()
            .put("userId", utility.getOwnerId())
            .put("userRole", "provider")
            .put("emailId", utility.getOwnerEmailId())
            .put("firstName", utility.getOwnerFirstName())
            .put("lastName", utility.getOwnerLastName());
    return new User(jsonObject);
  }

  @Test
  @DisplayName("Test sendEmail method : Success")
  public void testSendEmailSuccess(VertxTestContext vertxTestContext) {
    emailNotification
        .sendEmail(consumer, provider, utility.getResourceId().toString(), "rs.iudx.io")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(getDelegateEmailIds, times(1))
                    .getEmails(anyString(), anyString(), anyString());
                verify(mailResultAsyncResult, times(1)).succeeded();
                verify(mailClient, times(1)).sendMail(any());
                assertTrue(handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause().getMessage());
              }
            });
  }

  @Test
  @DisplayName("Test sendEmail method when notify by email is false")
  public void testSendEmailWithFalseNotifyByEmail(VertxTestContext vertxTestContext) {
    config.put("notifyByEmail", false);
    emailNotification = new EmailNotification(Vertx.vertx(), config, getDelegateEmailIds);

    emailNotification
        .sendEmail(consumer, provider, utility.getResourceId().toString(), "rs.iudx.io")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertTrue(handler.result());
                verify(mailClient, times(0)).sendMail(any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause().getMessage());
              }
            });
  }

  @Test
  @DisplayName("Test when there is a failure while fetching the delegate emailIds")
  public void testDuringFailure(VertxTestContext vertxTestContext) {
    when(getDelegateEmailIds.getEmails(anyString(), anyString(), anyString()))
        .thenReturn(jsonArrayFuture);
    doAnswer(
            new Answer<AsyncResult<JsonArray>>() {
              @Override
              public AsyncResult<JsonArray> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonArray>>) arg2.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(jsonArrayFuture)
        .onComplete(any(Handler.class));
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Something went wrong");

    emailNotification
        .sendEmail(consumer, provider, utility.getResourceId().toString(), "rs.iudx.io")
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Failed to fetch delegate email Ids", handler.cause().getMessage());
                verify(mailClient, times(0)).sendMail(any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(
                    "Succeeded when there is was a failure while fetching delegates");
              }
            });
  }

  @Test
  @DisplayName("Test when there is a failure while sending email")
  public void testDuringFailureWhileSendingEmails(VertxTestContext vertxTestContext) {
    when(mailClient.sendMail(any(MailMessage.class))).thenReturn(mailResultFuture);
    doAnswer(
            new Answer<AsyncResult<MailResult>>() {
              @Override
              public AsyncResult<MailResult> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<MailResult>>) arg2.getArgument(0))
                    .handle(mailResultAsyncResult);
                return null;
              }
            })
        .when(mailResultFuture)
        .onComplete(any(Handler.class));
    when(mailResultAsyncResult.succeeded()).thenReturn(false);
    when(mailResultAsyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Something went wrong");

    emailNotification
        .sendEmail(consumer, provider, utility.getResourceId().toString(), "rs.iudx.io")
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Something went wrong", handler.cause().getMessage());
                verify(mailClient, times(1)).sendMail(any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(
                    "Succeeded when there is was a failure while fetching delegates");
              }
            });
  }
}
