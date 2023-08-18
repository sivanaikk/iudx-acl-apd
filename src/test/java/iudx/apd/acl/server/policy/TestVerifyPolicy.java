package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.TITLE;
import static iudx.apd.acl.server.apiserver.util.Constants.TYPE;
import static iudx.apd.acl.server.common.HttpStatusCode.VERIFY_FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.Utility;
import iudx.apd.acl.server.apiserver.util.ResourceObj;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.policy.util.ItemType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestVerifyPolicy {
  private static final Logger LOGGER = LogManager.getLogger(TestCreatePolicy.class);

  @Container static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11");
  static CatalogueClient catalogueClient;
  private static VerifyPolicy verifyPolicy;
  private static Utility utility;

  @BeforeAll
  public static void setUp(VertxTestContext vertxTestContext) {
    utility = new Utility();
    PostgresService pgService = utility.setUp(container);

    utility
        .testInsert()
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                catalogueClient = Mockito.mock(CatalogueClient.class);
                verifyPolicy = new VerifyPolicy(pgService, catalogueClient);
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Failed to set up");
              }
            });
  }

  public JsonObject getRequest() {
    JsonObject user =
        new JsonObject()
            .put("id", utility.getConsumerId())
            .put(
                "name",
                new JsonObject()
                    .put("firstName", utility.getConsumerFirstName())
                    .put("lastName", utility.getConsumerLastName()))
            .put("email", utility.getConsumerEmailId());

    JsonObject owner =
        new JsonObject()
            .put("id", utility.getOwnerId())
            .put(
                "name",
                new JsonObject()
                    .put("firstName", utility.getOwnerFirstName())
                    .put("lastName", utility.getOwnerLastName()))
            .put("email", utility.getOwnerEmailId());

    JsonObject item =
        new JsonObject()
            .put("itemId", utility.getResourceId())
            .put("itemType", utility.getResourceType());

    JsonObject requestBody =
        new JsonObject().put("user", user).put("owner", owner).put("item", item);

    return requestBody;
  }

  @Test
  @DisplayName("Test initiateVerifyPolicy: Success")
  public void testInitiateVerifyPolicy(VertxTestContext vertxTestContext) {
    JsonObject request = getRequest();
    verifyPolicy
        .initiateVerifyPolicy(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(
                    ResponseUrn.VERIFY_SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
                assertEquals(new JsonObject(), handler.result().getJsonObject("constraints"));
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Failed");
              }
            });
  }

  @Test
  @DisplayName("Test initiateVerifyPolicy for resourceGroup: Fail")
  public void testInitiateVerifyPolicyFailForResourceGroup(VertxTestContext vertxTestContext) {
    JsonObject request = getRequest();
    JsonObject item =
        new JsonObject()
            .put("itemId", Utility.generateRandomUuid())
            .put("itemType", ItemType.RESOURCE_GROUP);
    request.put("item", item);

    verifyPolicy
        .initiateVerifyPolicy(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded by creating a policy");
              } else {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(VERIFY_FORBIDDEN.getValue(), result.getInteger(TYPE));
                assertEquals(VERIFY_FORBIDDEN.getUrn(), result.getString(TITLE));
                assertEquals(
                    "No policy exist for given Resource Group", result.getString("detail"));
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test initiateVerifyPolicy for resource: Success")
  public void testInitiateVerifyPolicyForResource(VertxTestContext vertxTestContext) {
    JsonObject request = getRequest();
    UUID mockResourceId = Utility.generateRandomUuid();

    JsonObject item =
        new JsonObject().put("itemId", mockResourceId).put("itemType", ItemType.RESOURCE);
    request.put("item", item);

    Set<UUID> mockUUIDList = new HashSet<>();
    mockUUIDList.add(mockResourceId);
    List<ResourceObj> resourceObjList = new ArrayList<>();
    ResourceObj resourceObj =
        new ResourceObj(
            Utility.generateRandomUuid(), utility.getOwnerId(), utility.getResourceId());
    resourceObjList.add(resourceObj);
    when(catalogueClient.fetchItems(mockUUIDList))
        .thenReturn(Future.succeededFuture(resourceObjList));

    verifyPolicy
        .initiateVerifyPolicy(request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(
                    ResponseUrn.VERIFY_SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
                assertEquals(new JsonObject(), handler.result().getJsonObject("constraints"));
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Failed");
              }
            });
  }

  @Test
  @DisplayName("Test initiateVerifyPolicy for resourceGroup not found in CAT: Fail")
  public void testInitiateVerifyPolicyFailForResource(VertxTestContext vertxTestContext) {
    JsonObject request = getRequest();
    UUID mockResourceId = Utility.generateRandomUuid();

    JsonObject item =
      new JsonObject()
        .put("itemId", mockResourceId)
        .put("itemType", ItemType.RESOURCE);
    request.put("item", item);

    Set<UUID> mockUUIDList = new HashSet<>();
    mockUUIDList.add(mockResourceId);

    when(catalogueClient.fetchItems(mockUUIDList)).thenReturn(Future.failedFuture(verifyPolicy.generateErrorResponse(
      VERIFY_FORBIDDEN, "Resource Group not found in CAT")));

    verifyPolicy
      .initiateVerifyPolicy(request)
      .onComplete(
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow("Succeeded by creating a policy");
          } else {
            JsonObject result = new JsonObject(handler.cause().getMessage());
            assertEquals(VERIFY_FORBIDDEN.getValue(), result.getInteger(TYPE));
            assertEquals(VERIFY_FORBIDDEN.getUrn(), result.getString(TITLE));
            assertEquals(
              "Resource Group not found in CAT", result.getString("detail"));
            vertxTestContext.completeNow();
          }
        });
    verify(catalogueClient).fetchItems(mockUUIDList);
  }

  @Test
  @DisplayName("Test initiateVerifyPolicy for no policy for given resource's Resource Group: Fail")
  public void testInitiateVerifyPolicyFailForResourceGrp(VertxTestContext vertxTestContext) {
    JsonObject request = getRequest();
    UUID mockResourceId = Utility.generateRandomUuid();

    JsonObject item =
      new JsonObject()
        .put("itemId", mockResourceId)
        .put("itemType", ItemType.RESOURCE);
    request.put("item", item);

    Set<UUID> mockUUIDList = new HashSet<>();
    mockUUIDList.add(mockResourceId);
    List<ResourceObj> resourceObjList = new ArrayList<>();
    ResourceObj resourceObj =
      new ResourceObj(
        Utility.generateRandomUuid(), utility.getOwnerId(), Utility.generateRandomUuid());
    resourceObjList.add(resourceObj);

    when(catalogueClient.fetchItems(mockUUIDList))
      .thenReturn(Future.succeededFuture(resourceObjList));

    verifyPolicy
      .initiateVerifyPolicy(request)
      .onComplete(
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow("Succeeded by creating a policy");
          } else {
            JsonObject result = new JsonObject(handler.cause().getMessage());
            assertEquals(VERIFY_FORBIDDEN.getValue(), result.getInteger(TYPE));
            assertEquals(VERIFY_FORBIDDEN.getUrn(), result.getString(TITLE));
            assertEquals(
              "No policy exist for given item's Resource Group", result.getString("detail"));
            vertxTestContext.completeNow();
          }
        });
  }

}
