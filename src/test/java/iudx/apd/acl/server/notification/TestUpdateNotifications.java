 package iudx.apd.acl.server.notification;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.Utility;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.policy.PostgresService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static iudx.apd.acl.server.Utility.generateRandomEmailId;
import static iudx.apd.acl.server.Utility.generateRandomString;
import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.*;
import static iudx.apd.acl.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static iudx.apd.acl.server.common.ResponseUrn.POLICY_ALREADY_EXIST_URN;
import static iudx.apd.acl.server.notification.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestUpdateNotifications {
    public static final String INSERT_INTO_USER_TABLE =
            "INSERT INTO user_table(_id, email_id, first_name, last_name) VALUES ($1, $2, $3, $4) RETURNING _id;";
    public static final String INSERT_INTO_RESOURCE_ENTITY_TABLE =
            "INSERT INTO resource_entity(_id, provider_id, resource_group_id) VALUES ($1, $2, $3) RETURNING _id;";
    private static final Logger LOG = LoggerFactory.getLogger(TestUpdateNotifications.class);
    private static final String INSERT_INTO_REQUEST_TABLE = "INSERT INTO request(_id, user_id, item_id, item_type, owner_id, status, expiry_at, constraints) VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING _id;";
    static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11");
    private static Utility utility;
    private static User owner;
    private static User consumer;
    private static UpdateNotification updateNotification;
    private static JsonObject rejectNotification;
    private static JsonObject approveNotification;
    private static UUID requestId;
    private static UUID consumerId;
    private static UUID resourceId;
    private static UUID ownerId;
    private static JsonObject constraints;
    private static String resourceType;
    private static String requestStatus;
    private static String expiryTime;
    private static String consumerEmailId;
    private static String ownerEmailId;
    private static String consumerFirstName;
    private static String consumerLastName;
    private static String ownerFirstName;
    private static String ownerLastName;
    private static LocalDateTime expiryAt;


    @BeforeAll
    public static void setUp(VertxTestContext vertxTestContext) {
        utility = new Utility();
        container.start();
        PostgresService pgService = utility.setUp(container);

        utility.testInsert().onComplete(handler -> {
            if (handler.succeeded()) {

                updateNotification = new UpdateNotification(pgService);
                rejectNotification = new JsonObject();
                approveNotification = new JsonObject();

                requestId = UUID.randomUUID();
                consumerId = UUID.randomUUID();
                resourceId = UUID.fromString("ae2b8b01-f642-411a-babb-cbd1b75fa2a1");
                ownerId = UUID.randomUUID();
                constraints = new JsonObject("{\"access\": [\"api\",\"sub\",\"file\"]}");
                resourceType = "RESOURCE";
                requestStatus = "PENDING";
                expiryTime = "2024-03-05T20:00:19";
                consumerEmailId = generateRandomEmailId();
                consumerFirstName = generateRandomString();
                consumerLastName = generateRandomString();

                ownerEmailId = generateRandomEmailId();
                ownerFirstName = generateRandomString();
                ownerLastName = generateRandomString();

                expiryAt = LocalDateTime.of(2025, 12, 10, 3, 20, 20, 29);
                rejectNotification.put("requestId", utility.getRequestId());
                rejectNotification.put("status", "rejected");

                approveNotification.put("requestId", requestId);
                approveNotification.put("status", "granted");
                approveNotification.put("expiryAt", expiryTime);
                approveNotification.put("constraints", constraints);

                Tuple consumerTuple =
                        Tuple.of(
                                consumerId, consumerEmailId, consumerFirstName, consumerLastName);
                Tuple ownerTuple =
                        Tuple.of(ownerId, ownerEmailId, ownerFirstName, ownerLastName);
                Tuple resourceInsertionTuple = Tuple.of(resourceId, ownerId, null);
                Tuple requestInsertionTuple = Tuple.of(requestId, consumerId, resourceId, resourceType, ownerId, requestStatus, expiryAt, constraints);

                owner = getOwner();
                consumer = getConsumer();

                utility.executeBatchQuery(List.of(ownerTuple, consumerTuple), INSERT_INTO_USER_TABLE);
                utility.executeQuery(resourceInsertionTuple, INSERT_INTO_RESOURCE_ENTITY_TABLE).onComplete(insertResourceHandler -> {
                    if (insertResourceHandler.succeeded()) {
                        utility.executeQuery(requestInsertionTuple, INSERT_INTO_REQUEST_TABLE).onComplete(insertRequestHandler -> {
                            if (insertRequestHandler.succeeded()) {
                                assertNotNull(updateNotification);
                                LOG.info("Set up the environment for testing successfully");
                                vertxTestContext.completeNow();
                            } else {
                                LOG.error("Failed to setup {}", insertRequestHandler.cause().getMessage());
                                vertxTestContext.failNow("Failed to insert request");
                            }
                        });
                    } else {
                        LOG.error("Failed to setup {}", insertResourceHandler.cause().getMessage());
                        vertxTestContext.failNow("Failed to insert resource");
                    }
                });

            } else {
                vertxTestContext.failNow("Failed to set up");
            }
        });
    }


    public static User getOwner() {
        JsonObject jsonObject = new JsonObject()
                .put("userId", ownerId)
                .put("userRole", "provider")
                .put("emailId", ownerEmailId)
                .put("firstName", ownerFirstName)
                .put("lastName", ownerLastName);
        return new User(jsonObject);
    }

    public static User getConsumer() {
        JsonObject jsonObject = new JsonObject()
                .put("userId", consumerId)
                .put("userRole", "consumer")
                .put("emailId", consumerEmailId)
                .put("firstName", consumerFirstName)
                .put("lastName", consumerLastName);
        return new User(jsonObject);
    }

    @Test
    @DisplayName("Test PUT notification : Success")
    public void testUpdateNotificationSuccess(VertxTestContext vertxTestContext) {
        updateNotification.initiateUpdateNotification(approveNotification, owner).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(ResponseUrn.SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
                assertEquals(ResponseUrn.SUCCESS_URN.getMessage(), handler.result().getString(TITLE));
                assertEquals("Request updated successfully", handler.result().getString(RESULT));
                assertEquals(HttpStatusCode.SUCCESS.getValue(), handler.result().getInteger(STATUS_CODE));
                vertxTestContext.completeNow();

            } else {
                LOG.error("Failed : {}", handler.cause().getMessage());
                vertxTestContext.failNow("Failed");
            }
        });
    }

    @Test
    @DisplayName("Test PUT notification by rejecting the request : Success")
    public void testUpdateRejectedNotification(VertxTestContext vertxTestContext) {
        JsonObject jsonObject = new JsonObject()
                .put("userId", utility.getOwnerId())
                .put("userRole", "provider")
                .put("emailId", utility.getOwnerEmailId())
                .put("firstName", utility.getOwnerFirstName())
                .put("lastName", utility.getOwnerLastName());
        User owner = new User(jsonObject);
        updateNotification.initiateUpdateNotification(rejectNotification, owner).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(ResponseUrn.SUCCESS_URN.getUrn(), handler.result().getString(TYPE));
                assertEquals(ResponseUrn.SUCCESS_URN.getMessage(), handler.result().getString(TITLE));
                assertEquals("Request updated successfully", handler.result().getString(RESULT));
                assertEquals(HttpStatusCode.SUCCESS.getValue(), handler.result().getInteger(STATUS_CODE));
                vertxTestContext.completeNow();
            } else {
                LOG.error("Failed : {}", handler.cause().getMessage());
                vertxTestContext.failNow("Failed");
            }
        });
    }

    @Test
    @DisplayName("Test initiateUpdateNotification method with request not being present : Failure")
    public void testInitiateUpdateNotificationWithNoRequests(VertxTestContext vertxTestContext) {
        updateNotification.initiateUpdateNotification(new JsonObject().put("requestId", UUID.randomUUID()).put("status", "rejected"), owner)
                .onComplete(handler -> {
                    if (handler.succeeded()) {
                        vertxTestContext.failNow("Succeeded when the request Id is not present");
                    } else {
                        JsonObject failureMessage = new JsonObject()
                                .put(TYPE, NOT_FOUND.getValue())
                                .put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                                .put(DETAIL, "Request could not be updated, as it is not found");
                        assertEquals(failureMessage.encode(), handler.cause().getMessage());
                        vertxTestContext.completeNow();

                    }
                });
    }

    @Test
    @DisplayName("Test initiateUpdateNotification method by rejecting an approved request : Failure")
    public void testInitiateUpdateNotification4ApprovedRequest(VertxTestContext vertxTestContext) {
        UUID requestId = UUID.randomUUID();
        JsonObject approveNotification = new JsonObject()
                .put("requestId", requestId)
                .put("status", "granted")
                .put("expiryAt", expiryTime)
                .put("constraints", constraints);
        JsonObject rejectNotification = new JsonObject()
                .put("requestId", requestId)
                .put("status", "rejected");

        Tuple resourceInsertionTuple = Tuple.of("83c2e5c2-3574-4e11-9530-2b1fbdfce832", ownerId, null);
        Tuple requestInsertionTuple = Tuple.of(requestId, consumerId, "83c2e5c2-3574-4e11-9530-2b1fbdfce832", resourceType, ownerId, requestStatus, expiryAt, constraints);
        utility.executeQuery(resourceInsertionTuple, INSERT_INTO_RESOURCE_ENTITY_TABLE).onComplete(handler -> {
            if (handler.succeeded()) {
                utility.executeQuery(requestInsertionTuple, INSERT_INTO_REQUEST_TABLE).onComplete(insertRequestHandler -> {
                    if (insertRequestHandler.succeeded()) {
                        updateNotification.initiateUpdateNotification(approveNotification, owner).onComplete(approvedRequestHandler -> {
                            if (approvedRequestHandler.succeeded()) {
                                updateNotification.initiateUpdateNotification(rejectNotification, owner).onComplete(rejectRequestHandler -> {
                                    if (rejectRequestHandler.succeeded()) {
                                        LOG.info("Succeeded for rejecting approved request");
                                        vertxTestContext.failNow("Succeeded for rejecting approved request");
                                    } else {
                                        JsonObject failureMessage = new JsonObject()
                                                .put(TYPE, HttpStatusCode.BAD_REQUEST.getValue())
                                                .put(TITLE, ResponseUrn.BAD_REQUEST_URN.getUrn())
                                                .put(DETAIL, "Request could not be updated, as it is not in pending status");
                                        assertEquals(failureMessage.encode(), rejectRequestHandler.cause().getMessage());
                                        vertxTestContext.completeNow();
                                    }
                                });
                            } else {
                                LOG.error("Failed : {}", approvedRequestHandler.cause().getMessage());
                                vertxTestContext.failNow("Failed to approve request");
                            }
                        });
                    } else {
                        LOG.error("Failed: {}", insertRequestHandler.cause().getMessage());
                        vertxTestContext.failNow("Failed to insert request");
                    }

                });
            }
        });
    }

    @Test
    @DisplayName("Test initiateUpdateNotification method by rejecting the requesting that isn't for the user")
    public void testRejectingRequestNotBelongingToOwner(VertxTestContext vertxTestContext) {
        updateNotification.initiateUpdateNotification(rejectNotification, consumer).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("Request ownership check failed");
            } else {
                JsonObject failureMessage = new JsonObject()
                        .put(TYPE, HttpStatusCode.FORBIDDEN.getValue())
                        .put(TITLE, ResponseUrn.FORBIDDEN_URN.getUrn())
                        .put(DETAIL, "Request could not be updated, as it is doesn't belong to the user");
                assertEquals(failureMessage.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test approve request by setting an invalid expiry time")
    public void testWithInvalidExpiryTime(VertxTestContext vertxTestContext) {
        UUID requestId = UUID.randomUUID();
        JsonObject approveNotification = new JsonObject()
                .put("requestId", requestId)
                .put("status", "granted")
                .put("expiryAt", "2020-03-05T20:00:19")
                .put("constraints", constraints);
        Tuple resourceInsertionTuple = Tuple.of("a347c5b6-5281-4749-9eab-89784d8f8f9a", ownerId, null);
        Tuple requestInsertionTuple = Tuple.of(requestId, consumerId, "a347c5b6-5281-4749-9eab-89784d8f8f9a", resourceType, ownerId, requestStatus, expiryAt, constraints);

        utility.executeQuery(resourceInsertionTuple, INSERT_INTO_RESOURCE_ENTITY_TABLE)
                .onComplete(resourceInsertionHandler -> {
                    if (resourceInsertionHandler.succeeded()) {
                        utility.executeQuery(requestInsertionTuple, INSERT_INTO_REQUEST_TABLE)
                                .onComplete(requestInsertionHandler -> {
                                    if (requestInsertionHandler.succeeded()) {
                                        updateNotification.initiateUpdateNotification(approveNotification, owner)
                                                .onComplete(handler -> {
                                                    if (handler.succeeded()) {
                                                        System.out.println("Success");
                                                        vertxTestContext.failNow("Succeeded for invalid expiryAt value");
                                                    } else {
                                                        JsonObject failureMessage = new JsonObject()
                                                                .put(TYPE, HttpStatusCode.BAD_REQUEST.getValue())
                                                                .put(TITLE, ResponseUrn.BAD_REQUEST_URN.getUrn())
                                                                .put(DETAIL, "Invalid expiry time, expiryAt could be greater than the present time");
                                                        assertEquals(failureMessage.encode(), handler.cause().getMessage());
                                                        vertxTestContext.completeNow();
                                                    }
                                                });
                                    } else {
                                        vertxTestContext.failNow("Failed to insert request : " + requestInsertionHandler.cause().getMessage());
                                    }

                                });
                    } else {
                        vertxTestContext.failNow("Failed to insert resource : " + resourceInsertionHandler.cause().getMessage());
                    }
                });
    }


    @Test
    @DisplayName("Test by rejecting a request that is expired")
    public void rejectExpiredNotification(VertxTestContext vertxTestContext) {
        UUID requestId = UUID.randomUUID();
        JsonObject rejectNotification = new JsonObject()
                .put("requestId", requestId)
                .put("status", "rejected");
        LocalDateTime expiryAt = LocalDateTime.of(2020, 12, 10, 3, 20, 20, 29);
        Tuple resourceInsertionTuple = Tuple.of("b58da193-23d9-43eb-b98a-a103d4b6103c", ownerId, null);
        Tuple requestInsertionTuple = Tuple.of(requestId, consumerId, "b58da193-23d9-43eb-b98a-a103d4b6103c", resourceType, ownerId, requestStatus, expiryAt, constraints);

        utility.executeQuery(resourceInsertionTuple, INSERT_INTO_RESOURCE_ENTITY_TABLE)
                .onComplete(resourceInsertionHandler -> {
                    if (resourceInsertionHandler.succeeded()) {
                        utility.executeQuery(requestInsertionTuple, INSERT_INTO_REQUEST_TABLE)
                                .onComplete(requestInsertionHandler -> {
                                    if (requestInsertionHandler.succeeded()) {
                                        updateNotification.initiateUpdateNotification(rejectNotification, owner)
                                                .onComplete(handler -> {
                                                    if (handler.succeeded()) {
                                                        System.out.println("Success");
                                                        vertxTestContext.failNow("Succeeded for invalid expiryAt value");
                                                    } else {
                                                        JsonObject failureMessage = new JsonObject()
                                                                .put(TYPE, HttpStatusCode.BAD_REQUEST.getValue())
                                                                .put(TITLE, ResponseUrn.BAD_REQUEST_URN.getUrn())
                                                                .put(DETAIL, "Request could not be rejected, as it is expired");
                                                        assertEquals(failureMessage.encode(), handler.cause().getMessage());
                                                        vertxTestContext.completeNow();
                                                    }
                                                });
                                    } else {
                                        vertxTestContext.failNow("Failed to insert request : " + requestInsertionHandler.cause().getMessage());
                                    }

                                });
                    } else {
                        vertxTestContext.failNow("Failed to insert resource : " + resourceInsertionHandler.cause().getMessage());
                    }
                });
    }


    @Test
    @DisplayName("Test by rejecting a request that is expired")
    public void testInitiateUpdateNotificationWithInvalidStatus(VertxTestContext vertxTestContext) {
        UUID requestId = UUID.randomUUID();
        JsonObject rejectNotification = new JsonObject()
                .put("requestId", requestId)
                .put("status", "expired");
        updateNotification.initiateUpdateNotification(rejectNotification, owner).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded for invalid status");
            } else {
                JsonObject failureMessage = new JsonObject()
                        .put(TYPE, HttpStatusCode.BAD_REQUEST.getValue())
                        .put(TITLE, ResponseUrn.BAD_REQUEST_URN.getUrn())
                        .put(DETAIL, "Invalid request status, request can be either rejected or granted");
                assertEquals(failureMessage.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test initiateUpdateNotification method by approving a request for which a policy is already created")
    public void testApproveNotificationWithPolicyAlreadyCreated(VertxTestContext vertxTestContext) {
        Utility utility1 = new Utility();
        PostgresService postgresService = utility1.setUp(container);
        utility1.testInsert().onComplete(setUpHandler -> {
            if (setUpHandler.succeeded()) {
                JsonObject approveNotification = new JsonObject()
                        .put("requestId", utility1.getRequestId())
                        .put("status", "granted")
                        .put("expiryAt", expiryTime)
                        .put("constraints", utility1.getConstraints());
                JsonObject jsonObject = new JsonObject()
                        .put("userId", utility1.getOwnerId())
                        .put("userRole", "provider")
                        .put("emailId", utility1.getOwnerEmailId())
                        .put("firstName", utility1.getOwnerFirstName())
                        .put("lastName", utility1.getOwnerLastName());
                User provider = new User(jsonObject);
                UpdateNotification updateNotification = new UpdateNotification(postgresService);
                updateNotification.initiateUpdateNotification(approveNotification, provider).onComplete(handler -> {
                    if (handler.succeeded()) {
                        vertxTestContext.failNow("Succeeded to approve request when policy is already created");
                    } else {
                        JsonObject failureMessage = new JsonObject()
                                .put(TYPE, HttpStatusCode.CONFLICT.getValue())
                                .put(TITLE, POLICY_ALREADY_EXIST_URN.getUrn())
                                .put(DETAIL, "Request cannot be approved as, policy is already created");
                        assertEquals(failureMessage.encode(), handler.cause().getMessage());
                        vertxTestContext.completeNow();
                    }
                });
            } else {
                LOG.error("Failed to setup : {}", setUpHandler.cause().getMessage());
                vertxTestContext.failNow("Failed to setup");
            }
        });
    }

    @Test
    @DisplayName("Test createPolicy method with invalid constraints")
    public void testCreatePolicyWithInvalidConstraint(VertxTestContext vertxTestContext) {
        JsonObject approveNotification = new JsonObject()
                .put("requestId", requestId)
                .put("status", "granted")
                .put("expiryAt", expiryTime)
                .put("constraints", "constraints");
        updateNotification.createPolicy(approveNotification, CREATE_POLICY_QUERY).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded for invalid constraint");
            } else {
                JsonObject failureMessage = new JsonObject()
                        .put(TYPE, BAD_REQUEST.getValue())
                        .put(TITLE, ResponseUrn.BAD_REQUEST_URN.getMessage())
                        .put(DETAIL, "Invalid constraints in the request body");
                assertEquals(failureMessage.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Test
    @DisplayName("Test initiateUpdateNotification with error while creating database connection")
    public void testWithDatabaseConnectionError(VertxTestContext vertxTestContext)
    {
        PostgresService postgresService = mock(PostgresService.class);
        PgPool pgPool = mock(PgPool.class);
        when(postgresService.getPool()).thenReturn(pgPool);
        when(pgPool.withConnection(any())).thenReturn(Future.failedFuture("Some error"));
        UpdateNotification updateNotification1 = new UpdateNotification(postgresService);
        updateNotification1.initiateUpdateNotification(approveNotification, owner).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded when there was a database error");
            } else {
                JsonObject failureMessage = new JsonObject()
                        .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                        .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                        .put(DETAIL, "Failure while executing query");
                assertEquals(failureMessage.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });

    }

    @Test
    @DisplayName("Test initiateUpdateNotification with NULL PgPool")
    public void testWithNullPgPool(VertxTestContext vertxTestContext)
    {
        PostgresService postgresService = mock(PostgresService.class);
        when(postgresService.getPool()).thenReturn(null);

        UpdateNotification updateNotification1 = new UpdateNotification(postgresService);
        assertThrows(NullPointerException.class, ()-> updateNotification1.initiateUpdateNotification(approveNotification, owner));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test checkIfPolicyExists method when consumer is invalid")
    public void testCheckIfPolicyExistsWithInvalidConsumer(VertxTestContext vertxTestContext)
    {
        updateNotification.setConsumerId(UUID.randomUUID());
        updateNotification.checkIfPolicyExists(GET_EXISTING_POLICY_QUERY).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("Succeeded in policy exists check when consumer is invalid");

            } else {
                JsonObject failureMessage = new JsonObject()
                        .put(TYPE, INTERNAL_SERVER_ERROR.getValue())
                        .put(TITLE, ResponseUrn.INTERNAL_SERVER_ERROR.getUrn())
                        .put(DETAIL, "Request cannot be approved as, consumer is not found");
                assertEquals(failureMessage.encode(),handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test checkOwner4GivenResource when the owner is invalid")
    public void testCheckOwner4GivenResource(VertxTestContext vertxTestContext)
    {
        updateNotification.setOwnerId(UUID.randomUUID());
        updateNotification.checkOwner4GivenResource(OWNERSHIP_CHECK_QUERY)
                .onComplete(handler -> {
                    if(handler.succeeded()){
                        vertxTestContext.failNow("Succeeded for invalid owner");
                    }
                    else
                    {
                        JsonObject failureMessage = new JsonObject()
                                .put(TYPE, FORBIDDEN.getValue())
                                .put(TITLE, FORBIDDEN.getUrn())
                                .put(DETAIL, "Request cannot be approved, as the resource does not belong to the provider");
                        assertEquals(failureMessage.encode(), handler.cause().getMessage());
                        vertxTestContext.completeNow();
                    }
                });
    }

    @Test
    @DisplayName("Test insertInApprovedAccessRequest method when the response from the DB is empty")
    public void testInsertInApprovedAccessRequest(VertxTestContext vertxTestContext)
    {
        updateNotification.setPolicyId(UUID.randomUUID());
        JsonObject approveNotification = new JsonObject()
                .put("requestId", requestId)
                .put("status", "granted")
                .put("expiryAt", expiryTime)
                .put("constraints", "constraints");
        updateNotification.insertInApprovedAccessRequest(approveNotification, INSERT_IN_APPROVED_ACCESS_REQUESTS_QUERY).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow("Succeeded with invalid requestId and while Database throws an error");
            }
            else
            {
                JsonObject failureMessage = new JsonObject()
                        .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                        .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                        .put(DETAIL, "Failure while executing query");
                assertEquals(failureMessage.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test approveNotification method when the response from the DB is empty")
    public void testApproveNotification(VertxTestContext vertxTestContext)
    {
        JsonObject approveNotification = new JsonObject()
                .put("requestId", UUID.randomUUID())
                .put("status", "granted")
                .put("expiryAt", expiryTime)
                .put("constraints", constraints);
        updateNotification.approveNotification(approveNotification, APPROVE_REQUEST_QUERY).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow("Succeeded with invalid requestId and while Database throws an error");
            }
            else
            {
                JsonObject failureMessage = new JsonObject()
                        .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                        .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                        .put(DETAIL, "Failure while executing query");
                assertEquals(failureMessage.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }


    @Test
    @DisplayName("Test createPolicy method when the response from the DB is empty")
    public void testCreatePolicy(VertxTestContext vertxTestContext)
    {
        updateNotification.setOwnerId(consumerId);
        JsonObject approveNotification = new JsonObject()
                .put("requestId", UUID.randomUUID())
                .put("status", "granted")
                .put("expiryAt", expiryTime)
                .put("constraints", constraints);
        updateNotification.createPolicy(approveNotification, CREATE_POLICY_QUERY).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow("Succeeded with invalid ownerId and while Database throws an error");
            }
            else
            {
                JsonObject failureMessage = new JsonObject()
                        .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                        .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                        .put(DETAIL, "Failure while executing query");
                assertEquals(failureMessage.encode(), handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }
}










































































































































