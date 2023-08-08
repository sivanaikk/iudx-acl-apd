package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.policy.util.Constants.GET_POLICY_4_CONSUMER_QUERY;
import static iudx.apd.acl.server.policy.util.Constants.GET_POLICY_4_PROVIDER_QUERY;
import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.apd.acl.server.Utility;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.common.ResponseUrn;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class TestGetPolicy {
  private static final Logger LOG = LoggerFactory.getLogger(TestGetPolicy.class);
  @Container static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11");
  private static GetPolicy getPolicy;
  private static User consumer;
  private static User provider;
  private static Utility utility;

  @BeforeAll
  public static void setUp(VertxTestContext vertxTestContext) {
    utility = new Utility();
    PostgresService postgresService = utility.setUp(container);
    utility
        .testInsert()
        .onSuccess(
            handler -> {
              consumer = getConsumer();
              provider = getProvider();
              getPolicy = new GetPolicy(postgresService);
              vertxTestContext.completeNow();
            })
        .onFailure(
            failureHandler -> {
              vertxTestContext.failNow(failureHandler.getCause().getMessage());
            });
  }

  private static User getConsumer() {
    JsonObject consumer =
        new JsonObject()
            .put("userId", utility.getConsumerId())
            .put("firstName", utility.getConsumerFirstName())
            .put("lastName", utility.getConsumerLastName())
            .put("emailId", utility.getConsumerEmailId())
            .put("userRole", "consumer");
    return new User(consumer);
  }

  private static User getProvider() {
    JsonObject provider =
        new JsonObject()
            .put("userId", utility.getOwnerId())
            .put("firstName", utility.getOwnerFirstName())
            .put("lastName", utility.getOwnerLastName())
            .put("emailId", utility.getOwnerEmailId())
            .put("userRole", "provider");
    return new User(provider);
  }

  @Test
  @DisplayName("Test getConsumerPolicy method")
  public void executeGetPolicy4Consumer(VertxTestContext vertxTestContext) {

    getPolicy
        .initiateGetPolicy(consumer)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject actualResult =
                    handler.result().getJsonObject(RESULT).getJsonArray(RESULT).getJsonObject(0);

                assertEquals(utility.getPolicyId().toString(), actualResult.getString("policy_id"));
                assertEquals(
                    utility.getConsumerEmailId(), actualResult.getString("consumer_email_id"));
                assertEquals(utility.getResourceId().toString(), actualResult.getString("item_id"));
                assertEquals(utility.getResourceType(), actualResult.getString("item_type"));
                assertEquals(utility.getOwnerId().toString(), actualResult.getString("owner_id"));
                assertEquals(
                    utility.getOwnerFirstName(), actualResult.getString("owner_first_name"));
                assertEquals(utility.getOwnerLastName(), actualResult.getString("owner_last_name"));
                assertEquals(utility.getOwnerEmailId(), actualResult.getString("owner_email_id"));
                assertEquals(utility.getStatus(), actualResult.getString("status"));
                assertNotNull(handler.result());
                assertTrue(handler.result().getJsonObject(RESULT).containsKey(TYPE));
                assertEquals(
                    ResponseUrn.SUCCESS_URN.getMessage(),
                    handler.result().getJsonObject(RESULT).getString(TITLE));
                assertEquals(200, handler.result().getInteger(STATUS_CODE));

                vertxTestContext.completeNow();
              } else {
                LOG.info("Failed");
                vertxTestContext.failNow(handler.cause().getMessage());
              }
            });
  }

  @Test
  @DisplayName("Test getConsumerPolicy with query having syntax error")
  public void testGetConsumerPolicy4InvalidQuery(VertxTestContext vertxTestContext) {
    getPolicy
        .getConsumerPolicy(consumer, GET_POLICY_4_CONSUMER_QUERY + "abcd")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded for invalid query");
              } else {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(500, result.getInteger(TYPE));
                assertEquals(ResponseUrn.SUCCESS_URN.getMessage(), result.getString(TITLE));
                assertEquals(
                    "Policy could not be fetched, Failure while executing query",
                    result.getString(DETAIL));
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test getConsumerPolicy method with null consumer")
  public void testGetConsumerPolicyWithNullUser(VertxTestContext vertxTestContext) {
    assertThrows(
        NullPointerException.class,
        () -> {
          getPolicy.getConsumerPolicy(null, GET_POLICY_4_CONSUMER_QUERY).onComplete(handler -> {});
        });
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test getConsumerPolicy method with invalid consumer details")
  public void testGetConsumerPolicyFailure(VertxTestContext vertxTestContext) {

    getPolicy
        .getConsumerPolicy(provider, GET_POLICY_4_CONSUMER_QUERY)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(404, result.getInteger(TYPE));
                assertTrue(result.containsKey(TITLE));
                assertEquals("Policy not found", result.getString(DETAIL));
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause().getMessage());
              }
            });
  }

  @Test
  @DisplayName("Test getProviderPolicy policy : Success")
  public void testGetProviderPolicySuccess(VertxTestContext vertxTestContext) {
    getPolicy
        .initiateGetPolicy(provider)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                JsonObject results = handler.result().getJsonObject(RESULT);
                JsonObject actualResult =
                    handler.result().getJsonObject(RESULT).getJsonArray(RESULT).getJsonObject(0);
                assertEquals(utility.getPolicyId().toString(), actualResult.getString("policy_id"));
                assertEquals(utility.getOwnerId().toString(), actualResult.getString("owner_id"));
                assertEquals(utility.getResourceId().toString(), actualResult.getString("item_id"));
                assertEquals(utility.getResourceType(), actualResult.getString("item_type"));
                assertEquals(
                    utility.getConsumerEmailId(), actualResult.getString("consumer_email_id"));
                assertEquals(
                    utility.getConsumerFirstName(), actualResult.getString("consumer_first_name"));
                assertEquals(
                    utility.getConsumerLastName(), actualResult.getString("consumer_last_name"));
                assertEquals(utility.getStatus(), actualResult.getString("status"));
                assertEquals(utility.getOwnerEmailId(), actualResult.getString("owner_email_id"));
                assertEquals(
                    utility.getOwnerFirstName(), actualResult.getString("owner_first_name"));
                assertEquals(utility.getOwnerLastName(), actualResult.getString("owner_last_name"));

                assertNotNull(handler.result());
                assertTrue(results.containsKey(TYPE));
                assertEquals(ResponseUrn.SUCCESS_URN.getMessage(), results.getString(TITLE));
                assertEquals(200, handler.result().getInteger(STATUS_CODE));
                vertxTestContext.completeNow();
              } else {
                LOG.error("Failure");
                vertxTestContext.failNow(handler.cause().getMessage());
              }
            });
  }

  @Test
  @DisplayName("Test getProviderPolicy method with invalid consumer details")
  public void testGetProviderPolicyFailure(VertxTestContext vertxTestContext) {

    getPolicy
        .getProviderPolicy(consumer, GET_POLICY_4_PROVIDER_QUERY)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(404, result.getInteger(TYPE));
                assertTrue(result.containsKey(TITLE));
                assertEquals("Policy not found", result.getString(DETAIL));
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause().getMessage());
              }
            });
  }

  @Test
  @DisplayName("Test getProviderPolicy method with null owner")
  public void testGetProviderPolicyWithNullUser(VertxTestContext vertxTestContext) {
    assertThrows(
        NullPointerException.class,
        () -> {
          getPolicy.getProviderPolicy(null, GET_POLICY_4_PROVIDER_QUERY).onComplete(handler -> {});
        });
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test get provider policy with query having syntax error")
  public void testGetProviderPolicy4InvalidQuery(VertxTestContext vertxTestContext) {
    getPolicy
        .getProviderPolicy(provider, GET_POLICY_4_PROVIDER_QUERY + "something")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded for invalid query");
              } else {
                JsonObject result = new JsonObject(handler.cause().getMessage());
                assertEquals(500, result.getInteger(TYPE));
                assertEquals(ResponseUrn.SUCCESS_URN.getMessage(), result.getString(TITLE));
                assertEquals(
                    "Policy could not be fetched, Failure while executing query",
                    result.getString(DETAIL));
                vertxTestContext.completeNow();
              }
            });
  }
}
