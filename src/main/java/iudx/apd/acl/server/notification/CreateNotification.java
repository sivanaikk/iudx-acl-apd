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
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.policy.CatalogueClient;
import iudx.apd.acl.server.policy.PostgresService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static iudx.apd.acl.server.apiserver.util.Constants.*;

public class CreateNotification {
    private static final Logger LOG = LoggerFactory.getLogger(CreateNotification.class);
    private static final String FAILURE_MESSAGE = "Request could not be created";
    private final PostgresService postgresService;
    private final CatalogueClient catalogueClient;
    private UUID providerId;
    private UUID resourceId;
    private String resourceType;
    private PgPool pool;

    public CreateNotification(PostgresService postgresService, CatalogueClient catalogueClient) {
        this.postgresService = postgresService;
        this.catalogueClient = catalogueClient;
    }


    public Future<JsonObject> initiateCreateNotification(JsonObject notification, User user) {
        resourceId = UUID.fromString(notification.getString("itemId"));
        resourceType = notification.getString("itemType");

        Future<Boolean> validPolicyExistsFuture = checkIfValidPolicyExists(resourceId, resourceType, user);
        Future<Boolean> validNotificationExistsFuture = validPolicyExistsFuture.compose(isValidPolicyExisting -> {
            /* Policy with ACTIVE status already present */
            if (isValidPolicyExisting) {
                return Future.failedFuture(validPolicyExistsFuture.cause().getMessage());
            }
            /* Policy doesn't exist, or is DELETED, or was expired */
            return checkIfValidNotificationExists(resourceId, resourceType, user);
        });

        Future<Boolean> checkIfResourceExistsInDbFuture = validNotificationExistsFuture.compose(isValidNotificationExisting -> {
            /*PENDING notification already exists waiting for its approval*/
            if (isValidNotificationExisting) {
                return Future.failedFuture(validNotificationExistsFuture.cause().getMessage());
            }
            /* Notification doesn't exist, or is WITHDRAWN, or REJECTED */
            /*TODO: what if a notification is approved but a policy is not created*/
            return checkIfResourceExistsInDb(resourceId);
        });
        Future<Boolean> insertNotificationFuture = checkIfResourceExistsInDbFuture.compose(isResourcePresentInDb -> {
            if (isResourcePresentInDb) {
                return createNotification(resourceId, resourceType, user, providerId);
            }
            Future<Boolean> checkResourceInCatFuture = isItemPresentInCatalogue(resourceId).compose(itemPresentInCat -> {
                if (itemPresentInCat) {
                    Future<Boolean> insertInDbFuture = insertResourceInDb(resourceId, resourceType, providerId).compose(resourceInsertedInDb -> {
                        if (resourceInsertedInDb) {
                            return createNotification(resourceId, resourceType, user, providerId);
                        }
                        JsonObject failureMessage = new JsonObject()
                                .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR)
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
            return Future.failedFuture(checkIfResourceExistsInDbFuture.cause().getMessage());
        });

        Future<JsonObject> userResponseFuture = insertNotificationFuture.compose(abdcifsjdf -> {
            if (abdcifsjdf) {
                return Future.succeededFuture(new JsonObject()
                        .put(TYPE, HttpStatusCode.SUCCESS)
                        .put(TITLE, ResponseUrn.SUCCESS_URN.getUrn())
                        .put(RESULT, "Request inserted successfully")
                        .put(STATUS_CODE, HttpStatusCode.SUCCESS.getValue()));
            }
            return Future.failedFuture(insertNotificationFuture.cause().getMessage());
        });
        return userResponseFuture;
    }

    public Future<Boolean> checkIfValidPolicyExists(UUID resourceId, String resourceType, User consumer) {
        Promise<Boolean> promise = Promise.promise();


        return promise.future();
    }

    public Future<Boolean> checkIfValidNotificationExists(UUID resourceId, String resourceType, User user) {
        Promise<Boolean> promise = Promise.promise();

        return promise.future();
    }

    public Future<Boolean> checkIfResourceExistsInDb(UUID resourceId) {
        Promise<Boolean> promise = Promise.promise();

        return promise.future();
    }

    public Future<Boolean> createNotification(UUID resourceId, String resourceType, User consumer, UUID providerId) {
        Promise<Boolean> promise = Promise.promise();

        return promise.future();
    }

    public Future<Boolean> isItemPresentInCatalogue(UUID resourceId) {
        Promise<Boolean> promise = Promise.promise();

        return promise.future();
    }

    public Future<Boolean> insertResourceInDb(UUID resourceId, String resourceType, UUID ownerId) {
        Promise<Boolean> promise = Promise.promise();

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

        pool = postgresService.getPool();
        Collector<Row, ?, List<JsonObject>> rowListCollector = Collectors.mapping(row -> row.toJson(), Collectors.toList());

        pool.withConnection(sqlConnection -> sqlConnection.preparedQuery(query).collecting(rowListCollector).execute(tuple).map(rows -> rows.value())).onSuccess(successHandler -> {
            JsonArray response = new JsonArray(successHandler);
            JsonObject responseJson = new JsonObject()
                    .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                    .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                    .put(RESULT, response);
            handler.handle(Future.succeededFuture(responseJson));
        }).onFailure(failureHandler -> {
            LOG.error("Failure while executing the query : {}", failureHandler.getMessage());
            JsonObject response = new JsonObject()
                    .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
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

}
