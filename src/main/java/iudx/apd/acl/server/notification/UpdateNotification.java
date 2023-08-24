package iudx.apd.acl.server.notification;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.apiserver.util.RequestStatus;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.policy.PostgresService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.apiserver.util.RequestStatus.PENDING;
import static iudx.apd.acl.server.common.HttpStatusCode.*;
import static iudx.apd.acl.server.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;
import static iudx.apd.acl.server.notification.util.Constants.*;

public class UpdateNotification {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateNotification.class);
    private final PostgresService postgresService;
    public PgPool pool;
    private UUID ownerId;
    private UUID itemId;
    private String itemType;
    private UUID consumerId;
    private String consumerEmailId;
    private UUID policyId;
    private LocalDateTime expiryAt;

    public UpdateNotification(PostgresService postgresService) {
        this.postgresService = postgresService;

    }

    /**
     * Updates the notification based on if the notification is being <b>GRANTED</b> or <b>REJECTED</b>
     * <br>
     * by letting the request related information and user related information go through multiple checks
     *
     * @param notification notification Request body from PUT notification API of type JsonObject
     * @param user         Information about the provider or provider delegate
     * @return Response as Future JsonObject
     */

    public Future<JsonObject> initiateUpdateNotification(JsonObject notification, User user) {
        LOG.trace("inside initiateUpdateNotification method");
        UUID notificationId = UUID.fromString(notification.getString("requestId"));
        /* get if the status is approved or rejected */
        /* check if status is valid */
        RequestStatus status = PENDING;
        try {
            status = RequestStatus.fromString(notification.getString("status"));
        } catch (Exception e) {
            LOG.error("Exception is {}", e.getMessage());
        }

        switch (status) {
            case REJECTED: {
                /* check if the request is valid */
                Future<Boolean> checkNotificationFuture = checkValidityOfRequest(GET_REQUEST, notificationId, user);
                Future<JsonObject> rejectedNotificationFuture = checkNotificationFuture.compose(isNotificationValid -> {
                    if (isNotificationValid) {
                        return updateRejectedNotification(REJECT_NOTIFICATION, notificationId);
                    }
                    return Future.failedFuture(checkNotificationFuture.cause().getMessage());
                });
                return rejectedNotificationFuture;
            }
            case GRANTED: {
                Future<Boolean> checkNotificationFuture = checkValidityOfRequest(GET_REQUEST, notificationId, user);

                /* checks the expiry time present in the request body is greater than the present time */
                Future<Boolean> verifiedExpiryTimeFuture = checkNotificationFuture.compose(isNotificationValid -> {
                    if (isNotificationValid) {
                        return checkExpiryTime(notification);
                    }
                    return Future.failedFuture(checkNotificationFuture.cause().getMessage());
                });
                /* Checks if a policy already exists in the policy table */
                Future<Boolean> verifyPolicyExistsFuture = verifiedExpiryTimeFuture.compose(isExpiryAtValid -> {
                    if (isExpiryAtValid) {
                        return checkIfPolicyExists(GET_EXISTING_POLICY_QUERY);
                    }
                    return Future.failedFuture(verifiedExpiryTimeFuture.cause().getMessage());
                });

                /* checks if the resource belongs to the provider */
                Future<Boolean> ownershipCheckFuture = verifyPolicyExistsFuture.compose(isPolicyExisting -> {
                    if (isPolicyExisting) {
                        return Future.failedFuture(verifyPolicyExistsFuture.cause().getMessage());
                    }
                    return checkOwner4GivenResource(OWNERSHIP_CHECK_QUERY);
                });
                Future<JsonObject> approvedRequestFuture = ownershipCheckFuture.compose(ownerOwningTheResource -> {
                    if (ownerOwningTheResource) {
                        return initiateTransactions(notification);
                    }
                    return Future.failedFuture(ownershipCheckFuture.cause().getMessage());
                });
                return approvedRequestFuture;
            }
            default: {
                JsonObject failureMessage = new JsonObject()
                        .put(TYPE, BAD_REQUEST.getValue())
                        .put(TITLE, ResponseUrn.BAD_REQUEST_URN.getUrn())
                        .put(DETAIL, "Invalid request status, request can be either rejected or granted");
                return Future.failedFuture(failureMessage.encode());
            }
        }
    }

    /**
     * Approves the notification or request by changing the status field to <b>GRANTED</b>, updating the constraints and expiry given
     * <br>
     * by the Provider or provider delegate
     *
     * @param notification Request body from PUT notification API of type JsonObject
     * @param query        UPDATE query
     * @return Success response or Failure response as Future JsonObject
     */
    public Future<JsonObject> approveNotification(JsonObject notification, String query) {
        LOG.trace("inside approveNotification method");
        Promise<JsonObject> promise = Promise.promise();
        UUID notificationId = UUID.fromString(notification.getString("requestId"));
        JsonObject constraints = new JsonObject(notification.getString("constraints"));

        Tuple tuple = Tuple.of(getExpiryAt(), constraints, notificationId, getOwnerId());
        executeQuery(query, tuple, handler -> {

            JsonObject result = handler.result().getJsonArray(RESULT).getJsonObject(0);
            boolean isResponseEmpty = result.isEmpty();
            /* if the id is not returned back after execution, then record is not inserted*/
            if (isResponseEmpty) {
                LOG.error("Could not update request table while approving the request as  : {}", handler.cause().getMessage());
                JsonObject failureMessage = new JsonObject()
                        .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                        .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                        .put(DETAIL, "Failure while executing query");
                promise.fail(failureMessage.encode());
            } else {

                JsonObject successResponse = new JsonObject()
                        .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                        .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                        .put(RESULT, "Request successfully approved!")
                        .put(STATUS_CODE, SUCCESS.getValue());
                promise.complete(successResponse);
            }
        });
        return promise.future();
    }

    /**
     * This method is used to initiate policy creation, inserting approved request and updating the approved request
     * <br>
     * one after the other and if there is any failure at any step, the whole transaction is rolled back
     * <br>
     * using vert.x pool.withTransaction method
     *
     * @param notification Request body from PUT notification API of type JsonObject
     * @return Final Success or Failure response to be presented to the user of type Future
     */
    public Future<JsonObject> initiateTransactions(JsonObject notification) {
        LOG.trace("inside initiateTransactions method");
        pool = postgresService.getPool();
        Future<JsonObject> transactionResponseFuture = pool.withTransaction(sqlConnection -> {
            Future<Boolean> createPolicyFuture = createPolicy(notification, CREATE_POLICY_QUERY);

            Future<Boolean> approvedAccessRequestsInsertionFuture = createPolicyFuture.compose(isPolicySuccessFullyCreated -> {
                if (isPolicySuccessFullyCreated) {
                    return insertInApprovedAccessRequest(notification, INSERT_IN_APPROVED_ACCESS_REQUESTS_QUERY);
                }
                return Future.failedFuture(createPolicyFuture.cause().getMessage());
            });

            Future<JsonObject> approvedNotificationFuture = approvedAccessRequestsInsertionFuture.compose(isSuccessFullyInserted -> {
                if (isSuccessFullyInserted) {
                    return approveNotification(notification, APPROVE_REQUEST_QUERY);
                }
                return Future.failedFuture(approvedAccessRequestsInsertionFuture.cause().getMessage());
            });
            return approvedNotificationFuture;
        });
        return transactionResponseFuture;
    }

    /**
     * Inserts a record in approved_access_request table by inserting the notificationId and policyId
     * <br>
     * when the notification is successfully approved or granted by the provider or provider delegate
     *
     * @param notification Request body from PUT notification API of type JsonObject
     * @param query        Insertion query
     * @return True, if the record is successfully inserted, failure if any in the form of Future
     */
    public Future<Boolean> insertInApprovedAccessRequest(JsonObject notification, String query) {
        LOG.trace("inside insertInApprovedAccessRequest method");
        Promise<Boolean> promise = Promise.promise();
        UUID id = UUID.randomUUID();
        UUID notificationId = UUID.fromString(notification.getString("requestId"));
        Tuple tuple = Tuple.of(id, notificationId, getPolicyId());
        executeQuery(query, tuple, handler -> {
            if(handler.succeeded()){
                JsonObject result = handler.result().getJsonArray(RESULT).getJsonObject(0);
                boolean isResponseEmpty = result.isEmpty();
                /* if the id is not returned back after execution, then record is not inserted*/
                if (isResponseEmpty) {
                    LOG.error("Could not insert in approved request access table as  : {}", handler.cause().getMessage());
                    JsonObject failureMessage = new JsonObject()
                            .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                            .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                            .put(DETAIL, "Failure while executing query");
                    promise.fail(failureMessage.encode());
                } else {
                    promise.complete(true);
                }
            }else
            {
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }

    /**
     * Checks if the given user who is requesting to approve the notification of the consumer is owning the resource
     *
     * @param query A SELECT query to check if the owner with given resource is present in resource entity table
     * @return True if the user is the owner, Provider/Provider delegate does not own the resource
     */
    public Future<Boolean> checkOwner4GivenResource(String query) {
        LOG.trace("inside checkOwner4GivenResource method");
        Promise<Boolean> promise = Promise.promise();
        Tuple tuple = Tuple.of(getItemId(), getOwnerId());
        executeQuery(query, tuple, handler -> {
            if (handler.succeeded()) {
                JsonObject result = handler.result().getJsonArray(RESULT).getJsonObject(0);
                boolean isResponseEmpty = result.isEmpty();
                /* if there are no records of a given item belonging the given provider, then ownership check failed*/
                if (isResponseEmpty) {
                    JsonObject failureMessage = new JsonObject()
                            .put(TYPE, FORBIDDEN.getValue())
                            .put(TITLE, FORBIDDEN.getUrn())
                            .put(DETAIL, "Request cannot be approved, as the resource does not belong to the provider");
                    promise.fail(failureMessage.encode());
                } else {
                    promise.complete(true);
                }
            }
        });
        return promise.future();
    }

    /**
     * Creates a policy by giving the information about consumer, owner, resource and expiry of the policy
     *
     * @param notification Request body as JsonObject
     * @param query        INSERT query to insert a new record for policy table
     * @return True, if a policy is successfully created, or failure if of type Future
     */
    public Future<Boolean> createPolicy(JsonObject notification, String query) {
        LOG.trace("inside createPolicy method");
        Promise<Boolean> promise = Promise.promise();
        UUID policyId = UUID.randomUUID();
        /* set policyId */
        setPolicyId(policyId);
        JsonObject constraints = null;
        try {
            constraints = new JsonObject(notification.getString("constraints"));
        }catch (DecodeException exception){
            LOG.error("Error : {}", exception.getMessage());
            JsonObject failureMessage = new JsonObject()
                    .put(TYPE, BAD_REQUEST.getValue())
                    .put(TITLE, ResponseUrn.BAD_REQUEST_URN.getMessage())
                    .put(DETAIL, "Invalid constraints in the request body");
            return Future.failedFuture(failureMessage.encode());
        }

            Tuple tuple = Tuple.of(policyId, getConsumerEmailId(), getItemId(), getItemType(), getOwnerId(), "ACTIVE", getExpiryAt(), constraints);

            executeQuery(query, tuple, handler -> {
                if (handler.succeeded()) {
                    JsonObject result = handler.result().getJsonArray(RESULT).getJsonObject(0);
                    if (!result.isEmpty()) {
                        /*policy is created successfully and the policy id is returned */
                        LOG.trace("Policy created successfully with policyId: {}", result.getString("_id"));
                        promise.complete(true);
                    } else {
                        LOG.error("Could not create policy : {}", handler.cause().getMessage());
                        JsonObject failureMessage = new JsonObject()
                                .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                                .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                                .put(DETAIL, "Failure while executing query");

                        promise.fail(failureMessage.encode());
                    }
                }else {
                    promise.fail(handler.cause().getMessage());
                }
            });
            return promise.future();

    }
    /**
     * Checks if the expiryAt value given the request body of the PUT Notification, is greater than the present time
     *
     * @param notification Request body as a JsonObject
     * @return True, if the expiryAt value given while approving the notification is valid or failure if not
     */
    public Future<Boolean> checkExpiryTime(JsonObject notification) {
        LOG.trace("inside checkExpiryTime method");
        String expiryAt = notification.getString("expiryAt");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.of("UTC"));
        LocalDateTime dateTime = LocalDateTime.parse(expiryAt, formatter);
        /* set expiryAt */
        setExpiryAt(dateTime);

        if (dateTime.isBefore(LocalDateTime.now())) {
            JsonObject failureMessage = new JsonObject()
                    .put(TYPE, BAD_REQUEST.getValue())
                    .put(TITLE, ResponseUrn.BAD_REQUEST_URN.getUrn())
                    .put(DETAIL, "Invalid expiry time, expiryAt could be greater than the present time");
            return Future.failedFuture(failureMessage.encode());
        }
        return Future.succeededFuture(true);
    }

    /**
     * Checks if the given policy is already present by considering the resourceId, resourceType, consumerEmailId and the ownerId
     * It also fetches emailId of the consumer based on the consumerId
     *
     * @param query A SELECT query to fetch policy if any, based on resource info, consumer and owner info
     * @return False if there is not a policy present previously, or failure if any of type Future JsonObject
     */
    public Future<Boolean> checkIfPolicyExists(String query) {
        LOG.trace("inside checkIfPolicyExists method");
        Promise<Boolean> promise = Promise.promise();
        Tuple consumerTuple = Tuple.of(getConsumerId());

        executeQuery(GET_CONSUMER_EMAIL_QUERY, consumerTuple, consumerEmailHandler -> {
            if (consumerEmailHandler.succeeded()) {
                JsonArray result = consumerEmailHandler.result().getJsonArray(RESULT);

                /* if the response is empty the consumer email is not found*/
                if (result.isEmpty()) {
                    JsonObject failureMessage = new JsonObject()
                            .put(TYPE, INTERNAL_SERVER_ERROR.getValue())
                            .put(TITLE, INTERNAL_SERVER_ERROR.getUrn())
                            .put(DETAIL, "Request cannot be approved as, consumer is not found");
                    promise.fail(failureMessage.encode());
                } else {
                    String consumerEmail = result.getJsonObject(0).getString("email_id");
                    setConsumerEmailId(consumerEmail);
                    Tuple tuple = Tuple.of(getOwnerId(), getItemId(), getItemType(), consumerEmail);
                    executeQuery(query, tuple, handler -> {
                        if (handler.succeeded()) {
                            JsonArray response = handler.result().getJsonArray(RESULT);
                            boolean isResponseEmpty = response.isEmpty();
                            /* if response is not empty then the policy is already present */
                            if (!isResponseEmpty) {
                                JsonObject failureMessage = new JsonObject()
                                        .put(TYPE, CONFLICT.getValue())
                                        .put(TITLE, CONFLICT.getUrn())
                                        .put(DETAIL, "Request cannot be approved as, policy is previously created");
                                promise.fail(failureMessage.encode());
                            } else {
                                promise.complete(false);
                            }
                        }
                    });
                }
            }
        });
        return promise.future();
    }

    /**
     * Checks validity of the given request by going through the respective checks
     * <br>
     * - If the request is found or not
     * <br>
     * - If the user requesting to approve or reject is the owner of the given request
     * <br>
     * - If the request is <b>PENDING</b> for it to be approved or rejected
     * <br>
     *
     * @param query        A SELECT query to get the request with the given requestId from the request body
     * @param notification requestId of type UUID
     * @param user         information about the provider or provider delegate of type User
     * @return True if the request is valid or the check which the request failed
     */
    public Future<Boolean> checkValidityOfRequest(String query, UUID notification, User user) {
        LOG.trace("inside checkValidityOfRequest method");
        Promise<Boolean> promise = Promise.promise();
        String ownerId = user.getUserId();
        Tuple tuple = Tuple.of(notification);
        executeQuery(query, tuple, handler -> {
            if (handler.succeeded()) {
                /* if the response is empty, notification is not found */
                if (handler.result().getJsonArray(RESULT).isEmpty()) {
                    JsonObject response = new JsonObject();
                    response.put(TYPE, NOT_FOUND.getValue());
                    response.put(TITLE, RESOURCE_NOT_FOUND_URN.getUrn());
                    response.put(DETAIL, "Request could not be granted or rejected, as it is not found");
                    promise.fail(response.encode());
                } else {
                    JsonObject response = handler.result().getJsonArray(RESULT).getJsonObject(0);
                    String owner = response.getString("owner_id");
                    String status = response.getString("status");
                    String consumer = response.getString("user_id");
                    String itemId = response.getString("item_id");
                    String itemType = response.getString("item_type");

                    /* ownership check */
                    if (owner.equals(ownerId)) {
                        /* check if status is in pending state */
                        if (status.equalsIgnoreCase("PENDING")) {
                            setOwnerId(UUID.fromString(owner));
                            setConsumerId(UUID.fromString(consumer));
                            setItemId(UUID.fromString(itemId));
                            setItemType(itemType);
                            promise.complete(true);
                        } else {
                            JsonObject failureResponse = new JsonObject();
                            failureResponse.put(TYPE, BAD_REQUEST.getValue());
                            failureResponse.put(TITLE, BAD_REQUEST.getUrn());
                            failureResponse.put(DETAIL, "Request could not be granted or rejected, as it is not in pending status");
                            promise.fail(failureResponse.encode());
                        }
                    } else {
                        JsonObject failureResponse = new JsonObject();
                        failureResponse.put(TYPE, FORBIDDEN.getValue());
                        failureResponse.put(TITLE, FORBIDDEN.getUrn());
                        failureResponse.put(DETAIL, "Request could not be granted or rejected, as it is doesn't belong to the user");
                        promise.fail(failureResponse.encode());
                    }
                }

            } else {
                LOG.error("Failed {}", handler.cause().getMessage());
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }

    /**
     * Updates the request by setting the status to <b>REJECTED</b>
     *
     * @param query        Query to update the given request
     * @param notification requestId to work on of type UUID
     * @return JsonObject of type Future to associate success and failure response
     */
    public Future<JsonObject> updateRejectedNotification(String query, UUID notification) {
        LOG.trace("inside updateRejectedNotification method");
        Promise<JsonObject> promise = Promise.promise();
        Tuple tuple = Tuple.of(notification);
        executeQuery(query, tuple, handler -> {
            if (handler.succeeded()) {
                if (!handler.result().getJsonArray(RESULT).isEmpty()) {
                    JsonObject response = new JsonObject()
                            .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                            .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                            .put(RESULT, "Request rejected successfully!")
                            .put(STATUS_CODE, SUCCESS.getValue());
                    promise.complete(response);
                } else {
                    LOG.trace("Notification has expired ");
                    JsonObject failureResponse = new JsonObject();
                    failureResponse.put(TYPE, BAD_REQUEST.getValue());
                    failureResponse.put(TITLE, BAD_REQUEST.getUrn());
                    failureResponse.put(DETAIL, "Request could not be rejected, as it is expired");
                    promise.fail(failureResponse.encode());
                }
            } else {
                LOG.info("Failed : {}", handler.cause().getMessage());
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }


    /**
     * Executes the query by getting the Pgpool instance from postgres
     *
     * @param query   to be executes
     * @param tuple   exchangeable values to be added in the query
     * @param handler
     */
    public void executeQuery(String query, Tuple tuple, Handler<AsyncResult<JsonObject>> handler) {
        LOG.trace("inside executeQuery method");
        pool = postgresService.getPool();
        Collector<Row, ?, List<JsonObject>> rowListCollector = Collectors.mapping(row -> row.toJson(), Collectors.toList());

        pool.withConnection(sqlConnection -> sqlConnection.preparedQuery(query).collecting(rowListCollector).execute(tuple).map(rows -> rows.value())).onSuccess(successHandler -> {
            JsonArray response = new JsonArray(successHandler);
            JsonObject responseJson = new JsonObject().put(TYPE, ResponseUrn.SUCCESS_URN.getUrn()).put(TITLE, ResponseUrn.SUCCESS_URN.getMessage()).put(RESULT, response);
            handler.handle(Future.succeededFuture(responseJson));
        }).onFailure(failureHandler -> {
            LOG.error("Failure while executing the query : {},{}", failureHandler.getMessage(), query);
            JsonObject response = new JsonObject().put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue()).put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn()).put(DETAIL, "Failure while executing query");
            handler.handle(Future.failedFuture(response.encode()));
        });
    }


    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public UUID getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(UUID consumerId) {
        this.consumerId = consumerId;
    }

    public String getConsumerEmailId() {
        return consumerEmailId;
    }

    public void setConsumerEmailId(String emailId) {
        consumerEmailId = emailId;
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public void setPolicyId(UUID policyId) {
        this.policyId = policyId;
    }

    public LocalDateTime getExpiryAt() {
        return expiryAt;
    }

    public void setExpiryAt(LocalDateTime expiryAt) {
        this.expiryAt = expiryAt;
    }
}
