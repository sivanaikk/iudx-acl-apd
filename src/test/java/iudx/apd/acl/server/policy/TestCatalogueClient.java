package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
import iudx.apd.acl.server.apiserver.util.ResourceObj;
import java.util.Set;
import java.util.UUID;

import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestCatalogueClient {

  CatalogueClient catalogueClient;
  JsonObject options;
  WebClient webClient;
  @Mock HttpRequest<Buffer> bufferHttpRequest;
  @Mock HttpResponse<Buffer> bufferHttpResponse;
  @Mock Future<HttpResponse<Buffer>> future;
  @Mock Throwable throwable;
  Set<UUID> uuidSet;
  JsonObject result;
  @Mock AsyncResult<HttpResponse<Buffer>> asyncResult;
  String resourceId;
  JsonArray jsonArray;
  JsonArray resourceGroup;
  JsonArray providerJsonArray;
  JsonArray resourceServerJsonArray;
  String ownerId;
  private static final Logger LOGGER = LogManager.getLogger(TestCatalogueClient.class);

  @BeforeEach
  public void init(VertxTestContext vertxTestContext) {
    options =
        new JsonObject()
            .put("catServerHost", "someCatServerHost")
            .put("catServerPort", 433)
            .put(APD_URL, "someDummyUrl")
            .put("dxCatalogueBasePath", "someBasePath");
    resourceId = "b2c27f3f-2524-4a84-816e-91f9ab23f837";
    ownerId = Utility.generateRandomUuid().toString();


    result = new JsonObject();
    jsonArray = new JsonArray();
    resourceGroup = new JsonArray();
    providerJsonArray = new JsonArray();
    resourceServerJsonArray = new JsonArray();
    providerJsonArray.add("iudx:Provider");
    resourceGroup.add("iudx:ResourceGroup");
    resourceGroup.add("iudx:TransitManagement");
    resourceServerJsonArray.add("iudx:ResourceServer");
    jsonArray.add(new JsonObject().put("type", resourceGroup).put("id", Utility.generateRandomUuid()));
    jsonArray.add(new JsonObject().put("type", providerJsonArray).put("ownerUserId", ownerId).put(ID, Utility.generateRandomUuid()));
    jsonArray.add(
        new JsonObject()
            .put("type", resourceServerJsonArray)
                .put(ID, Utility.generateRandomUuid())
            .put(RS_URL, "rs.iudx.io"));
    result.put(TYPE, CAT_SUCCESS_URN);
    result.put(RESULT, jsonArray);

    uuidSet = Set.of(UUID.fromString(resourceId));
    catalogueClient = new CatalogueClient(options);
    catalogueClient.client = mock(WebClient.class);
    webClient = catalogueClient.client;
    lenient().when(webClient.get(anyInt(), anyString(), anyString())).thenReturn(bufferHttpRequest);
    lenient()
        .when(bufferHttpRequest.addQueryParam(anyString(), anyString()))
        .thenReturn(bufferHttpRequest);
    lenient().when(bufferHttpRequest.send()).thenReturn(Future.succeededFuture(bufferHttpResponse));
    lenient()
        .doAnswer(
            new Answer<HttpResponse<Buffer>>() {
              @Override
              public HttpResponse<Buffer> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<HttpResponse<Buffer>>) arg2.getArgument(0)).handle(bufferHttpResponse);
                return null;
              }
            })
        .when(future)
        .onSuccess(any());

    lenient().when(bufferHttpResponse.bodyAsJsonObject()).thenReturn(result);
    lenient().when(asyncResult.succeeded()).thenReturn(true);
    lenient().when(asyncResult.result()).thenReturn(bufferHttpResponse);
    lenient().when(bufferHttpResponse.bodyAsJsonObject()).thenReturn(result);

    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test fetchItems with group level resource: Failure")
  public void testFetchItemsWithGroupLevelResource(VertxTestContext vertxTestContext) {
    jsonArray.add(new JsonObject().put("type", resourceGroup).put("id", resourceId));
    catalogueClient
        .fetchItems(uuidSet)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                JsonObject response = new JsonObject(handler.cause().getMessage());
                assertEquals(HttpStatusCode.BAD_REQUEST.getValue(), response.getInteger(TYPE));
                assertEquals(ResponseUrn.BAD_REQUEST_URN.getUrn(), response.getString(TITLE));
                assertEquals("Given id is invalid - it is group level resource", response.getString(DETAIL));
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Succeeded when the resource passed is of group level");
              }
            });
  }

  @Test
  @DisplayName("Test fetchItems : Success")
  public void testFetchItems(VertxTestContext vertxTestContext) {
    JsonArray resourceJsonArray = new JsonArray()
            .add("iudx:Resource");
    jsonArray.add(new JsonObject().put("type", resourceJsonArray).put(APD_URL, "someDummyUrl"));

    catalogueClient
            .fetchItems(uuidSet)
            .onComplete(
                    handler -> {
                      if (handler.succeeded()) {
                        LOGGER.info("RESULT "+handler.result().get(0).getItemId());
                        ResourceObj result = handler.result().get(0);
                        assertEquals(resourceId, result.getItemId().toString());
                        assertEquals(ownerId, result.getProviderId().toString());
                        assertEquals("rs.iudx.io", result.getResourceServerUrl());
                        assertFalse(result.getIsGroupLevelResource());
                        vertxTestContext.completeNow();

                      } else {
                        vertxTestContext.failNow(handler.cause().getMessage());
                      }
                    });
  }
  @Test
  @DisplayName("Test fetchItems : Failure")
  public void testFetchItemsFailure(VertxTestContext vertxTestContext) {
    when(bufferHttpRequest.send()).thenReturn(Future.failedFuture(throwable));
    when(throwable.getMessage()).thenReturn("Some failure while fetching the Item from CAT");

    catalogueClient
        .fetchItems(uuidSet)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(INTERNAL_SERVER_ERROR.getDescription(), handler.cause().getMessage());
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Succeeded for failed response from Catalogue");
              }
            });
  }

  @Test
  @DisplayName("Test fetchItems when Catalogue responds with Bad request error: Failure")
  public void testFetchItemsWithBadRequest(VertxTestContext vertxTestContext) {
    result.put(TYPE, "urn:dx:cat:badRequest");
    result.put(DETAIL, "Bad request");

    when(bufferHttpResponse.bodyAsJsonObject()).thenReturn(result);

    catalogueClient
        .fetchItems(uuidSet)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Bad request", handler.cause().getMessage());
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Succeeded for 400 - bad request from Catalogue");
              }
            });
  }
}
