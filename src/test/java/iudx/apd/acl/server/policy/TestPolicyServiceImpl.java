package iudx.apd.acl.server.policy;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
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

import static iudx.apd.acl.server.apiserver.util.Constants.EMAIL_ID;
import static iudx.apd.acl.server.apiserver.util.Constants.FIRST_NAME;
import static iudx.apd.acl.server.apiserver.util.Constants.LAST_NAME;
import static iudx.apd.acl.server.apiserver.util.Constants.USER;
import static iudx.apd.acl.server.apiserver.util.Constants.USER_ID;
import static iudx.apd.acl.server.apiserver.util.Constants.USER_ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestPolicyServiceImpl {
  @Container static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11");
  @Mock DeletePolicy deletePolicy;
  @Mock CreatePolicy createPolicy;
  @Mock GetPolicy getPolicy;
  @Mock VerifyPolicy verifyPolicy;
  Utility utility;
  User consumer;
  User provider;
  @Mock JsonObject request;
  @Mock Future<JsonObject> future;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Throwable throwable;
  JsonObject jsonObject;
  PolicyServiceImpl service;
  JsonObject config;

  @BeforeEach
  public void testSetUp(VertxTestContext vertxTestContext) {
    config = new JsonObject().put("defaultExpiryDays", 1000L);
    jsonObject = new JsonObject().put("result", "someResponse");

    utility = new Utility();
    consumer = getConsumer();
    provider = getOwner();
    service = new PolicyServiceImpl(deletePolicy, createPolicy, getPolicy, verifyPolicy, config);

    lenient().when(createPolicy.initiateCreatePolicy(any(), any())).thenReturn(future);
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
    lenient().when(deletePolicy.initiateDeletePolicy(any(), any())).thenReturn(future);
    lenient().when(getPolicy.initiateGetPolicy(any())).thenReturn(future);
    lenient().when(verifyPolicy.initiateVerifyPolicy(any())).thenReturn(future);
    vertxTestContext.completeNow();
  }

  public User getConsumer() {
    JsonObject jsonObject =
        new JsonObject()
            .put(USER_ID, Utility.generateRandomString())
            .put(USER_ROLE, "consumer")
            .put(EMAIL_ID, Utility.generateRandomEmailId())
            .put(FIRST_NAME, Utility.generateRandomString())
            .put(LAST_NAME, Utility.generateRandomString());
    return new User(jsonObject);
  }

  public User getOwner() {
    JsonObject jsonObject =
        new JsonObject()
            .put(USER_ID, Utility.generateRandomString())
            .put(USER_ROLE, "provider")
            .put(EMAIL_ID, Utility.generateRandomEmailId())
            .put(FIRST_NAME, Utility.generateRandomString())
            .put(LAST_NAME, Utility.generateRandomString());
    return new User(jsonObject);
  }

  private void callFailureMocks() {
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Some Failure Message");
  }

  @Test
  @DisplayName("Test createPolicy : Success")
  public void testCreatePolicySuccess(VertxTestContext vertxTestContext) {

    service
        .createPolicy(request, consumer)
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
  @DisplayName("Test deletePolicy : Success")
  public void testDeletePolicySuccess(VertxTestContext vertxTestContext) {

    service
        .deletePolicy(request, consumer)
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
  @DisplayName("Test getPolicy : Success")
  public void testGetPolicySuccess(VertxTestContext vertxTestContext) {

    service
        .getPolicy(consumer)
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
  @DisplayName("Test verifyPolicy : Success")
  public void testVerifyPolicySuccess(VertxTestContext vertxTestContext) {

    service
        .verifyPolicy(jsonObject)
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
  @DisplayName("Test createPolicy : Failure")
  public void testCreatePolicyFailure(VertxTestContext vertxTestContext) {
    callFailureMocks();

    service
        .createPolicy(request, consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when creating policy failed");
              } else {
                assertEquals("Some Failure Message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test deletePolicy : Failure")
  public void testDeletePolicyFailure(VertxTestContext vertxTestContext) {
    callFailureMocks();
    service
        .deletePolicy(request, consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when deleting policy failed");
              } else {
                assertEquals("Some Failure Message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test getPolicy : Failure")
  public void testGetPolicyFailure(VertxTestContext vertxTestContext) {
    callFailureMocks();
    service
        .getPolicy(consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when fetching policy failed");
              } else {
                assertEquals("Some Failure Message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test verifyPolicy : Failure")
  public void testVerifyPolicyFailure(VertxTestContext vertxTestContext) {
    callFailureMocks();
    service
        .verifyPolicy(jsonObject)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when verify policy failed");

              } else {
                assertEquals("Some Failure Message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }
}
