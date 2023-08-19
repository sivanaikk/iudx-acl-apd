package iudx.apd.acl.server.notification;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.apiserver.util.Role;
import iudx.apd.acl.server.apiserver.util.User;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import iudx.apd.acl.server.policy.PostgresService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.apd.acl.server.notification.util.Constants.GET_CONSUMER_NOTIFICATION_QUERY;
import static iudx.apd.acl.server.notification.util.Constants.GET_PROVIDER_NOTIFICATION_QUERY;


public class GetNotification {
    private final PostgresService postgresService;
    private static final Logger LOG = LoggerFactory.getLogger(GetNotification.class);
    private static final String FAILURE_MESSAGE = "Notifications could not be fetched";
    private PgPool pool;
    public GetNotification(PostgresService postgresService){
        this.postgresService = postgresService;
    }

    /**
     * Fetches notifications based on the user role
     * @param user Detail about the user
     * @return Notification or Failure with type Future JsonObject
     */
    public Future<JsonObject> initiateGetNotifications(User user){
        Role role = user.getUserRole();
        switch (role) {
            case CONSUMER_DELEGATE:
            case CONSUMER:
                return getConsumerNotification(user, GET_CONSUMER_NOTIFICATION_QUERY);
            case PROVIDER_DELEGATE:
            case PROVIDER:
                return getProviderNotification(user, GET_PROVIDER_NOTIFICATION_QUERY);
            default: {
                JsonObject response =
                        new JsonObject()
                                .put(TYPE, BAD_REQUEST.getValue())
                                .put(TITLE, BAD_REQUEST.getUrn())
                                .put(DETAIL, "Invalid role");
                return Future.failedFuture(response.encode());
            }
        }
    }

    /**
     * Fetches notifications for the respective provider when the user is provider or provider delegate
     * @param provider Information of provider with type User
     * @param query A SELECT query to be executed
     * @return notifications as success response or failure both of type Future JsonObject
     */
    public Future<JsonObject> getProviderNotification(User provider, String query) {
        LOG.trace("inside getProviderNotification method");
        Promise<JsonObject> promise = Promise.promise();
        UUID owner_id = UUID.fromString(provider.getUserId());
        LOG.trace(provider.toString());
        Tuple tuple = Tuple.of(owner_id);
        JsonObject jsonObject = new JsonObject()
                .put("email", provider.getEmailId())
                .put("name", new JsonObject().put("firstName", provider.getFirstName()).put("lastName", provider.getLastName()))
                .put("id", provider.getUserId());
        JsonObject providerInfo = new JsonObject().put("provider", jsonObject);
        this.executeGetNotification(tuple, query, providerInfo, Role.PROVIDER)
                .onComplete(
                        handler -> {
                            if (handler.succeeded()) {
                                LOG.info("success while executing GET provider request");
                                promise.complete(handler.result());
                            } else {
                                LOG.error("failure while executing GET provider request");
                                promise.fail(handler.cause().getMessage());
                            }
                        });
        return promise.future();
    }


    /**
     * Fetches the notifications concerned with the respective consumer when the user requesting it is a consumer or consumer delegate
     * @param consumer Information about the consumer with type User
     * @param query A SELECT query to fetch details
     * @return notifications from the consumer as success response or failure response both of type Future JsonObject
     */
    public Future<JsonObject> getConsumerNotification(User consumer, String query) {
        LOG.trace("inside getConsumerNotification method");
        Promise<JsonObject> promise = Promise.promise();
        UUID consumerId = UUID.fromString(consumer.getUserId());
        LOG.trace(consumer.toString());
        Tuple tuple = Tuple.of(consumerId);
        JsonObject jsonObject = new JsonObject()
                .put("email", consumer.getEmailId())
                .put("name", new JsonObject().put("firstName", consumer.getFirstName()).put("lastName", consumer.getLastName()))
                .put("id", consumer.getUserId());
        JsonObject consumerInfo = new JsonObject().put("consumer", jsonObject);

        this.executeGetNotification(tuple, query, consumerInfo, Role.CONSUMER)
                .onComplete(
                        handler -> {
                            if (handler.succeeded()) {
                                LOG.info("success while executing GET consumer request");
                                promise.complete(handler.result());
                            } else {
                                LOG.error("Failure while executing GET consumer request");
                                promise.fail(handler.cause().getMessage());
                            }
                        });
        return promise.future();
    }

    /**
     * Executes GET notification query based on the role of the user
     * @param tuple replaceable in the query with type Tuple
     * @param query to be executed
     * @param information to be merged with the response
     * @param role user role
     * @return response returned from the query execution
     */
    private Future<JsonObject> executeGetNotification(Tuple tuple, String query, JsonObject information, Role role) {
        Promise<JsonObject> promise = Promise.promise();
        Collector<Row, ?, List<JsonObject>> rowListCollector =
                Collectors.mapping(row -> row.toJson(), Collectors.toList());
        pool = postgresService.getPool();
        pool.withConnection(
                        sqlConnection ->
                                sqlConnection
                                        .preparedQuery(query)
                                        .collecting(rowListCollector)
                                        .execute(tuple)
                                        .map(rows -> rows.value()))
                .onComplete(
                        handler -> {
                            if (handler.succeeded()) {
                                if (handler.result().size() > 0) {
                                    for (JsonObject jsonObject : handler.result()) {
                                        jsonObject.mergeIn(information).mergeIn(getInformation(jsonObject, role));
                                    }
                                    JsonObject result =
                                            new JsonObject()
                                                    .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                                                    .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                                                    .put(RESULT, handler.result());

                                    promise.complete(
                                            new JsonObject()
                                                    .put(RESULT, result)
                                                    .put(STATUS_CODE, HttpStatusCode.SUCCESS.getValue()));
                                } else {
                                    JsonObject response = new JsonObject()
                                            .put(TYPE, HttpStatusCode.NOT_FOUND.getValue())
                                            .put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn())
                                            .put(DETAIL, "Request not found");
                                    LOG.error("No Request found!");
                                    promise.fail(response.encode());
                                }
                            } else {
                                JsonObject response = new JsonObject()
                                        .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                                        .put(TITLE, ResponseUrn.DB_ERROR_URN.getMessage())
                                        .put(DETAIL, FAILURE_MESSAGE + ", Failure while executing query");
                                promise.fail(response.encode());
                                LOG.error("Error response : {}", handler.cause().getMessage());
                            }
                        });
        return promise.future();
    }

    public JsonObject getInformation(JsonObject jsonObject, Role role) {
        if (role.equals(Role.CONSUMER)) {
            return getConsumerInformation(jsonObject);
        }
        return getProviderInformation(jsonObject);
    }

    public JsonObject getConsumerInformation(JsonObject jsonObject) {
        String ownerFirstName = jsonObject.getString("ownerFirstName");
        String ownerLastName = jsonObject.getString("ownerLastName");
        String ownerId = jsonObject.getString("ownerId");
        String ownerEmail = jsonObject.getString("ownerEmailId");
        JsonObject providerJson = new JsonObject()
                .put("email", ownerEmail)
                .put("name", new JsonObject().put("firstName", ownerFirstName).put("lastName", ownerLastName))
                .put("id", ownerId);
        JsonObject providerInfo = new JsonObject().put("provider", providerJson);
        jsonObject.remove("ownerFirstName");
        jsonObject.remove("ownerLastName");
        jsonObject.remove("ownerId");
        jsonObject.remove("ownerEmailId");
        return providerInfo;
    }

    public JsonObject getProviderInformation(JsonObject jsonObject) {
        String consumerFirstName = jsonObject.getString("consumerFirstName");
        String consumerLastName = jsonObject.getString("consumerLastName");
        String consumerId = jsonObject.getString("consumerId");
        String consumerEmail = jsonObject.getString("consumerEmailId");
        JsonObject consumerJson = new JsonObject()
                .put("email", consumerEmail)
                .put("name", new JsonObject().put("firstName", consumerFirstName).put("lastName", consumerLastName))
                .put("id", consumerId);
        JsonObject consumerInfo = new JsonObject().put("consumer", consumerJson);
        jsonObject.remove("consumerFirstName");
        jsonObject.remove("consumerLastName");
        jsonObject.remove("consumerId");
        jsonObject.remove("consumerEmailId");
        return consumerInfo;
    }
}
