package iudx.apd.acl.server.notification;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.Utility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestGetDelegateEmailIds {
  GetDelegateEmailIds getDelegateEmailIds;
  @Mock WebClient webClient;
  @Mock AsyncResult<HttpResponse<Buffer>> responseHandler;
  @Mock HttpRequest<Buffer> bufferHttpRequest;
  @Mock HttpResponse<Buffer> bufferHttpResponse;
  @Mock Future<HttpResponse<Buffer>> httpResponseFuture;
  @Mock JsonObject jsonObject;
  @Mock JsonArray jsonArray;
  @Mock Throwable throwable;
  JsonObject config;
  String userId;
  String resourceServerUrl;
  String role;

  @BeforeEach
  public void init(VertxTestContext vertxTestContext) {
    config =
        new JsonObject()
            .put("clientId", UUID.randomUUID().toString())
            .put("clientSecret", "someClientSecret")
            .put("authHost", "someAuthHost")
            .put("dxAuthBasePath", "someBasePath")
            .put("authPort", 443);
    userId = Utility.generateRandomString();
    resourceServerUrl = "rs.iudx.io";
    role = "provider";
    getDelegateEmailIds = new GetDelegateEmailIds(config, webClient);
    lenient().when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(bufferHttpRequest);
    lenient()
        .when(bufferHttpRequest.addQueryParam(anyString(), anyString()))
        .thenReturn(bufferHttpRequest);
    lenient()
        .when(bufferHttpRequest.putHeader(anyString(), anyString()))
        .thenReturn(bufferHttpRequest);
    lenient()
        .doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg2)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg2.getArgument(0))
                    .handle(responseHandler);
                return null;
              }
            }).when(bufferHttpRequest).send(any(Handler.class));
    lenient().when(responseHandler.succeeded()).thenReturn(true);
    lenient().when(responseHandler.result()).thenReturn(bufferHttpResponse);
    lenient().when(bufferHttpResponse.bodyAsJsonObject()).thenReturn(jsonObject);
    lenient().when(jsonObject.getJsonObject(anyString())).thenReturn(jsonObject);
    lenient().when(jsonObject.isEmpty()).thenReturn(false);
    lenient().when(jsonObject.getJsonArray(anyString())).thenReturn(jsonArray);
    lenient().when(jsonArray.isEmpty()).thenReturn(false);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test getEmails method : Success")
  public void testGetEmails(VertxTestContext vertxTestContext) {
    getDelegateEmailIds
        .getEmails(userId, resourceServerUrl, role)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                  assertEquals(jsonArray, handler.result());
                  verify(bufferHttpRequest, times(3)).addQueryParam(anyString(),anyString());
                  vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause().getMessage());
              }
            });
  }

  @Test
  @DisplayName("Test getEmails method : Failure")
  public void testGetEmailsFailure(VertxTestContext vertxTestContext)
  {
    when(responseHandler.succeeded()).thenReturn(false);
    when(responseHandler.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Something went wrong");
    getDelegateEmailIds.getEmails(userId, resourceServerUrl, role).onComplete(handler -> {
      if(handler.succeeded())
      {
        vertxTestContext.failNow("Succeeded when failed request was sent");
      }
      else
      {
        assertEquals("Something went wrong while fetching email Ids", handler.cause().getMessage());
        vertxTestContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("Test when response is null")
  public void testWhenResponseIsNull(VertxTestContext vertxTestContext)
  {
    lenient().when(jsonObject.getJsonObject(anyString())).thenReturn(null);

    getDelegateEmailIds.getEmails(userId, resourceServerUrl, role).onComplete(handler -> {
      if(handler.succeeded())
      {
        vertxTestContext.failNow("Succeeded when response is null");
      }
      else
      {
        assertEquals("Could not fetch email Ids", handler.cause().getMessage());
        vertxTestContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("Test when email Id is not present in the response")
  public void testWhenUserHasNoDelegates(VertxTestContext vertxTestContext)
  {
    lenient().when(jsonArray.isEmpty()).thenReturn(true);

    getDelegateEmailIds.getEmails(userId, resourceServerUrl, role).onComplete(handler -> {
      if(handler.succeeded())
      {
        vertxTestContext.failNow("Succeeded when no delegate email ids are present");
      }
      else
      {
        assertEquals("No delegates for the given provider", handler.cause().getMessage());
        vertxTestContext.completeNow();
      }
    });
  }
}
