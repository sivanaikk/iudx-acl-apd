package iudx.apd.acl.server.notification;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.apiserver.util.ResourceObj;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.policy.CatalogueClient;
import iudx.apd.acl.server.policy.PostgresService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static iudx.apd.acl.server.notification.util.Constants.*;


public class CreateNotification {
    private static final Logger LOG = LoggerFactory.getLogger(CreateNotification.class);
    private static final String FAILURE_MESSAGE = "Request could not be created";
    private final PostgresService postgresService;
    private final CatalogueClient catalogueClient;
    private UUID providerId;
    private UUID resourceId;
    private UUID resourceGroupId;
    private String resourceType;
    private PgPool pool;

    public CreateNotification(PostgresService postgresService, CatalogueClient catalogueClient) {
        this.postgresService = postgresService;
        this.catalogueClient = catalogueClient;
    }

    /**
     * Initiates the process of creating notifications by letting the request information go through multiple checks
     * @param notification request body for the POST Notification API with type JsonObject
     * @param user details of the consumer
     * @return response as JsonObject with type Future
     */

    public Future<JsonObject> initiateCreateNotification(JsonObject notification, User user) {
        resourceId = UUID.fromString(notification.getString("itemId"));
        resourceType = notification.getString("itemType");

        Future<Boolean> validPolicyExistsFuture = checkIfValidPolicyExists(GET_ACTIVE_CONSUMER_POLICY,resourceId, resourceType, user);
        Future<Boolean> validNotificationExistsFuture = validPolicyExistsFuture.compose(isValidPolicyExisting -> {
            /* Policy with ACTIVE status already present */
            if (isValidPolicyExisting) {
                return Future.failedFuture(validPolicyExistsFuture.cause().getMessage());
            }
            /* Policy doesn't exist, or is DELETED, or was expired */
            return checkIfValidNotificationExists(GET_VALID_NOTIFICATION, resourceId, resourceType, user);
        });

        Future<Boolean> checkIfResourceExistsInDbFuture = validNotificationExistsFuture.compose(isValidNotificationExisting -> {
            /*PENDING notification already exists waiting for its approval*/
            if (isValidNotificationExisting) {
                return Future.failedFuture(validNotificationExistsFuture.cause().getMessage());
            }
            /* Notification doesn't exist, or is WITHDRAWN, or REJECTED */
            /*TODO: what if a notification is approved but a policy is not created*/
            return checkIfResourceExistsInDb(GET_RESOURCE_INFO_QUERY, resourceId);
        });

        Future<Boolean> insertNotificationFuture = checkIfResourceExistsInDbFuture.compose(isResourcePresentInDb -> {
            if (isResourcePresentInDb) {
                return createNotification(CREATE_NOTIFICATION_QUERY, resourceId, resourceType, user, providerId);
            }
               return isItemPresentInCatalogue(resourceId).compose(itemPresentInCat -> {
                    if (itemPresentInCat) {
                        return insertResourceInDb(INSERT_RESOURCE_IN_DB_QUERY, resourceId, getResourceGroupId(), getProviderId()).compose(resourceInsertedInDb -> {
                            if (resourceInsertedInDb) {
                                return createNotification(CREATE_NOTIFICATION_QUERY, resourceId, resourceType, user, providerId);
                            }
                            JsonObject failureMessage = new JsonObject()
                                    .put(TYPE, INTERNAL_SERVER_ERROR)
                                    .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                                    .put(DETAIL, FAILURE_MESSAGE + ", as an error occurred in DB while inserting resource");
                            return Future.failedFuture(failureMessage.encode());
                        });
                    }
                    JsonObject failureMessage = new JsonObject()
                            .put(TYPE, HttpStatusCode.NOT_FOUND)
                            .put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                            .put(DETAIL, FAILURE_MESSAGE + ", as resource is not found");
                    return Future.failedFuture(failureMessage.encode());
                });
        });

        Future<JsonObject> userResponseFuture = insertNotificationFuture.compose(isNotificationSuccessfullyInserted -> {
            if (isNotificationSuccessfullyInserted) {
                return Future.succeededFuture(new JsonObject()
                        .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                        .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                        .put(RESULT, "Request inserted successfully")
                        .put(STATUS_CODE, HttpStatusCode.SUCCESS.getValue()));
            }
            return Future.failedFuture(insertNotificationFuture.cause().getMessage());
        });
        return userResponseFuture;
    }

    /**
     * checks if the policy for the given resource and given consumer already exists or not
     * <br>
     * If it is existing checks if it has been <b>DELETED</b> status or <b>EXPIRED</b>
     * <br>
     *  If the policy is in <b>ACTIVE</b> status then failure response is returned back
     * @param query A SELECT query to fetch details about policy
     * @param resourceId id of the resource with type UUID
     * @param resourceType type of the resource, can be <b>RESOURCE</b> or <b>RESOURCE_GROUP</b>
     * @param consumer Details of the user requesting to create notification with type User
     * @return False if policy is not present, <b>DELETED</b>, or <b>EXPIRED</b>. Failure if it is in <b>ACTIVE</b> status
     */
    public Future<Boolean> checkIfValidPolicyExists(String query, UUID resourceId, String resourceType, User consumer) {
        Promise<Boolean> promise = Promise.promise();
        LOG.trace("inside checkIfValidPolicyExists method");
        String consumerEmail = consumer.getEmailId();
        Tuple tuple = Tuple.of(consumerEmail, resourceId, resourceType);
        executeQuery(query, tuple, handler -> {
            if (handler.succeeded()) {
                JsonArray result = handler.result().getJsonArray(RESULT);
                boolean isPolicyAbsent = result.isEmpty();
                if (isPolicyAbsent) {
                    promise.complete(false);
                } else
                    /* An active policy for the consumer is present */ {
                    JsonObject failureMessage = new JsonObject()
                            .put(TYPE, HttpStatusCode.CONFLICT.getValue())
                            .put(TITLE, HttpStatusCode.CONFLICT.getUrn())
                            .put(DETAIL, FAILURE_MESSAGE + ", as a policy is already present");
                    promise.fail(failureMessage.encode());
                }
            } else {
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }

    /**
     * Verifies if the notification is already present by checking if it is <b>PENDING</b> status and if the resource has
     * <br>
     * been previously requested by the given user
     * @param query A SELECT query to fetch details about the notification
     * @param resourceId  id of the resource with type UUID
     * @param resourceType type of the resource, can be <b>RESOURCE</b> or <b>RESOURCE_GROUP</b>
     * @param user consumer details with type User
     * @return false if the notification was not previously created, failure response if the notification was previously created
     */
    public Future<Boolean> checkIfValidNotificationExists(String query, UUID resourceId, String resourceType, User user) {
        Promise<Boolean> promise = Promise.promise();
        LOG.trace("inside checkIfValidNotificationExists method");
        UUID consumerId = UUID.fromString(user.getUserId());
        Tuple tuple = Tuple.of(consumerId, resourceId, resourceType);
        executeQuery(query, tuple, handler -> {
            if(handler.succeeded()){
                JsonArray result = handler.result().getJsonArray(RESULT);
                boolean isNotificationAbsent = result.isEmpty();
                if(isNotificationAbsent){
                    promise.complete(false);
                }else {
                    /* A notification was created previously by the consumer and is in PENDING status */
                    JsonObject failureResponse = new JsonObject()
                            .put(TYPE, HttpStatusCode.CONFLICT.getValue())
                            .put(TITLE, HttpStatusCode.CONFLICT.getUrn())
                            .put(DETAIL, FAILURE_MESSAGE + ", as a request for the given resource has been previously made");
                    promise.fail(failureResponse.encode());
                }
            }
            else
            {
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }

    /**
     * Verifies if the resource being requested for accessing, is present in the database
     * @param query To fetch the details of the resource
     * @param resourceId id of the resource with type UUID
     * @return True, if the resource is found in the database or failure if any
     */
    public Future<Boolean> checkIfResourceExistsInDb(String query, UUID resourceId) {
        Promise<Boolean> promise = Promise.promise();
        LOG.trace("inside checkIfResourceExistsInDb method");
        Tuple tuple = Tuple.of(resourceId);
        executeQuery(query, tuple, handler -> {
            if(handler.succeeded()){
                JsonArray result = handler.result().getJsonArray(RESULT);
                boolean isResourcePresent = !(result.isEmpty());
                if(isResourcePresent){
                    /* get the information of the provider of the resource */
                    String provider = result.getJsonObject(0).getString("provider_id");
                    setProviderId(UUID.fromString(provider));

                    promise.complete(true);
                }
                else
                {
                    /* the given resource is not present in the resource entity table*/
                    promise.complete(false);
                }
            }
            else
            {
                promise.fail(handler.cause().getMessage());
            }
        });

        return promise.future();
    }

    /**
     * Creates notification for the consumer to access the given resource
     * @param query Insert query to create notification
     * @param resourceId id for which the consumer or consumer delegate wants access to with type UUID
     * @param resourceType type of the resource, can be <b>RESOURCE</b> or <b>RESOURCE_GROUP</b>
     * @param consumer details of the consumer with type User
     * @param providerId id of the owner of the resource with type UUID
     * @return True, if notification is created successfully, failure if any
     */
    public Future<Boolean> createNotification(String query, UUID resourceId, String resourceType, User consumer, UUID providerId) {
        Promise<Boolean> promise = Promise.promise();
        LOG.trace("inside createNotification method");
        UUID notificationId = UUID.randomUUID();
        UUID consumerId = UUID.fromString(consumer.getUserId());
        Tuple tuple = Tuple.of(notificationId, consumerId, resourceId, resourceType, providerId);
        executeQuery(query, tuple, handler -> {
            if (handler.succeeded()) {
                JsonArray result = handler.result().getJsonArray(RESULT);
                if (result.isEmpty()) {
                    /*notification id not returned*/
                    JsonObject failureMessage = new JsonObject()
                            .put(TYPE, INTERNAL_SERVER_ERROR.getValue())
                            .put(TITLE, INTERNAL_SERVER_ERROR.getUrn())
                            .put(DETAIL, FAILURE_MESSAGE);
                    promise.fail(failureMessage.encode());
                } else {
                    LOG.info("created a notification with notification Id : {}",result.getJsonObject(0).getString("_id"));
                    promise.complete(true);
                }
            } else {
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }

    /**
     * Checks if the item given in the request is present in Catalogue. It will get the information related to resource like
     * <br>
     * resourceGroupId, providerId and stores these values to be used further
     * @param resourceId or itemId of the given resource with type UUID
     * @return True, if information is fetched successfully, failure if there is no resource in the CAT with the given id
     * or if any other failure occurs
     */
    public Future<Boolean> isItemPresentInCatalogue(UUID resourceId) {
        Promise<Boolean> promise = Promise.promise();
        LOG.trace("inside isItemPresentInCatalogue method");
        Set<UUID> uuidSet = Set.of(resourceId);
        catalogueClient.fetchItems(uuidSet).onComplete(handler -> {
            if (handler.succeeded()) {
                ResourceObj result = handler.result().get(0);
                UUID ownerId = result.getProviderId();
                UUID resourceGroupIdValue = result.getResourceGroupId();

                /*set provider id and resourceGroupId */
                setProviderId(ownerId);
                setResourceGroupId(resourceGroupIdValue);

                promise.complete(true);
            } else {
                if (handler.cause().getMessage().equalsIgnoreCase("Id/Ids does not present in CAT")) {
                    /*id not present in the catalogue*/
                    JsonObject failureMessage = new JsonObject()
                            .put(TYPE, HttpStatusCode.NOT_FOUND.getValue())
                            .put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                            .put(DETAIL, FAILURE_MESSAGE + ", as resource was not found in the catalogue");
                    promise.fail(failureMessage.encode());
                } else {
                    /*something went wrong while fetching the item from catalogue*/
                    LOG.error("Failure while fetching item from CAT : {}", handler.cause().getMessage());
                    JsonObject failureMessage = new JsonObject()
                            .put(TYPE, INTERNAL_SERVER_ERROR.getValue())
                            .put(TITLE, INTERNAL_SERVER_ERROR.getUrn())
                            .put(DETAIL, FAILURE_MESSAGE);
                    promise.fail(failureMessage.encode());
                }
            }
        });
        return promise.future();
    }

    /**
     * Inserts a new resource in resource_entity table
     * @param query Insert query to insert the resource
     * @param resourceId id of the resource with type UUID
     * @param resourceGroupId resourceGroupId if present, for the resource with type UUID or null
     * @param ownerId id of the provider of the resource with type UUID
     * @return True if insertion is successful, failure if any
     */
    public Future<Boolean> insertResourceInDb(String query, UUID resourceId, UUID resourceGroupId, UUID ownerId) {
        Promise<Boolean> promise = Promise.promise();
        LOG.trace("inside insertResourceInDb method");
        Tuple tuple = Tuple.of(resourceId, ownerId, resourceGroupId);
        executeQuery(query, tuple, handler -> {
            if(handler.succeeded()){
                JsonArray result = handler.result().getJsonArray(RESULT);
                if(result.isEmpty()){
                    /* id is not returned while inserting the resource in the table*/
                    LOG.error("Something went wrong while inserting the resources in the table");
                    JsonObject failureMessage = new JsonObject()
                            .put(TYPE, INTERNAL_SERVER_ERROR.getValue())
                            .put(TITLE, INTERNAL_SERVER_ERROR.getUrn())
                            .put(DETAIL, FAILURE_MESSAGE);
                    promise.fail(failureMessage.encode());
                }
                else
                {
                    /*successfully inserted resources in the table*/
                    promise.complete(true);
                }
            }else {
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

        pool.withConnection(sqlConnection ->
                        sqlConnection.preparedQuery(query)
                        .collecting(rowListCollector)
                        .execute(tuple).map(rows -> rows.value()))
                .onSuccess(successHandler -> {
            JsonArray response = new JsonArray(successHandler);
            JsonObject responseJson = new JsonObject()
                    .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                    .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                    .put(RESULT, response);
            handler.handle(Future.succeededFuture(responseJson));
        }).onFailure(failureHandler -> {
            LOG.error("Failure while executing the query : {}", failureHandler.getMessage());
            JsonObject response = new JsonObject()
                    .put(TYPE, INTERNAL_SERVER_ERROR.getValue())
                    .put(TITLE, ResponseUrn.DB_ERROR_URN.getMessage())
                    .put(DETAIL, "Failure while executing query");
            handler.handle(Future.failedFuture(response.encode()));
        });
    }
    public UUID getProviderId() {
        return providerId;
    }

    public void setProviderId(UUID ownerId) {
        providerId = ownerId;
    }

    public UUID getResourceGroupId(){
        return resourceGroupId;
    }

    public void setResourceGroupId(UUID resourceGroupId){
        this.resourceGroupId = resourceGroupId;
    }

}
