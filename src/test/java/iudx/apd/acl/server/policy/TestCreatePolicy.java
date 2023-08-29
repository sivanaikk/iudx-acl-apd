package iudx.apd.acl.server.policy;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import iudx.apd.acl.server.Utility;
import iudx.apd.acl.server.apiserver.util.ResourceObj;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
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

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.apd.acl.server.common.HttpStatusCode.FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestCreatePolicy {
  private static final Logger LOGGER = LogManager.getLogger(TestCreatePolicy.class);

  @Container static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11");
  static CatalogueClient catalogueClient;
  private static CreatePolicy createPolicy;
  private static Utility utility;
  private static User owner;

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
                owner = getOwner();
                createPolicy = new CreatePolicy(pgService, catalogueClient);
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
  public static JsonObject getRequest(String userEmail,UUID resourceId) {
    JsonObject jsonObject =
        new JsonObject()
            .put("itemId", resourceId)
            .put("itemType", utility.getResourceType())
            .put("userEmail", userEmail)
            .put("constraints", new JsonObject().put("access", new JsonArray().add("sub")));

    return new JsonObject()
        .put("request", new JsonArray().add(jsonObject))
        .put("defaultExpiryDays", 12);
  }

  @Test
  @DisplayName("Test initiateCreatePolicy : Success")
  public void testInitiateCreatePolicy(VertxTestContext vertxTestContext) {
    JsonObject request = getRequest(Utility.generateRandomEmailId(),utility.getResourceId());

    createPolicy
        .initiateCreatePolicy(request, owner)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(ResponseUrn.SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
                assertEquals(
                    ResponseUrn.SUCCESS_URN.getMessage(), handler.result().getString(TITLE));
                assertTrue(
                    handler
                        .result()
                        .getJsonArray("result")
                        .getJsonObject(0)
                        .containsKey("policyId"));
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow("Failed");
              }
            });
  }

  @Test
  @DisplayName("Test initiateCreatePolicy with an already existing policy")
  public void testInitiateCreatePolicy4ExistingPolicy(VertxTestContext vertxTestContext) {
    JsonObject request = getRequest(utility.getConsumerEmailId(),utility.getResourceId());
    createPolicy
        .initiateCreatePolicy(request, owner)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded by creating a policy");
              } else {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(HttpStatusCode.CONFLICT.getValue(), result.getInteger(TYPE));
                assertEquals(HttpStatusCode.CONFLICT.getUrn(), result.getString(TITLE));
                assertTrue(result.containsKey("detail"));
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test initiateCreatePolicy with an Ownership Error")
  public void testInitiateCreatePolicy4OwnerShipError(VertxTestContext vertxTestContext) {
    JsonObject request = getRequest(utility.getConsumerEmailId(),utility.getResourceId());
    JsonObject provider =
        new JsonObject()
            .put("userId", Utility.generateRandomUuid())
            .put("firstName", utility.getOwnerFirstName())
            .put("lastName", utility.getOwnerLastName())
            .put("emailId", utility.getOwnerEmailId())
            .put("userRole", "provider");
    User providerUser = new User(provider);

    createPolicy
        .initiateCreatePolicy(request, providerUser)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded by creating a policy");
              } else {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(FORBIDDEN.getValue(), result.getInteger(TYPE));
                assertEquals(FORBIDDEN.getUrn(), result.getString(TITLE));
                assertEquals("Access Denied: You do not have ownership rights for this resource.", result.getString("detail"));
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test initiateCreatePolicy where resource is not present in resource_entity table: Success")
  public void testInitiateCreatePolicyCatCall(VertxTestContext vertxTestContext) {
    Set<UUID> mockUUIDList = new HashSet<>();
    UUID mockResourceId = Utility.generateRandomUuid();
    mockUUIDList.add(mockResourceId);
    JsonObject request = getRequest(Utility.generateRandomEmailId(),mockResourceId);
    List<ResourceObj> resourceObjList = new ArrayList<>();

    ResourceObj resourceObj = new ResourceObj(mockResourceId,utility.getOwnerId(),null,
      Utility.generateRandomUrl());
    resourceObjList.add(resourceObj);
    when(catalogueClient.fetchItems(mockUUIDList)).thenReturn(Future.succeededFuture(resourceObjList));

    createPolicy
      .initiateCreatePolicy(request, owner)
      .onComplete(
        handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseUrn.SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
            assertEquals(
              ResponseUrn.SUCCESS_URN.getMessage(), handler.result().getString(TITLE));
            assertTrue(
              handler
                .result()
                .getJsonArray("result")
                .getJsonObject(0)
                .containsKey("policyId"));
            vertxTestContext.completeNow();

          } else {
            vertxTestContext.failNow("Failed");
          }
        });
  }

  @Test
  @DisplayName("Test initiateCreatePolicy where resource is not present in resource_entity table: Fail")
  public void testInitiateCreatePolicyCatCallFailure(VertxTestContext vertxTestContext) {
    Set<UUID> mockUUIDList = new HashSet<>();
    UUID mockResourceId = Utility.generateRandomUuid();

    mockUUIDList.add(mockResourceId);
    JsonObject request = getRequest(Utility.generateRandomEmailId(),mockResourceId);
    when(catalogueClient.fetchItems(mockUUIDList)).thenReturn(Future.failedFuture("Id/Ids does not present in CAT"));

    createPolicy
      .initiateCreatePolicy(request, owner)
      .onComplete(
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow("Succeeded by creating a policy");
          } else {
            JsonObject result = new JsonObject(handler.cause().getMessage());
            assertEquals(BAD_REQUEST.getValue(), result.getInteger(TYPE));
            assertEquals(BAD_REQUEST.getUrn(), result.getString(TITLE));
            assertEquals("Bad Request", result.getString("detail"));
            vertxTestContext.completeNow();
          }
        });
  }
}
