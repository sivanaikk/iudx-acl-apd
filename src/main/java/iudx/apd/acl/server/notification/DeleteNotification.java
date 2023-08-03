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
import iudx.apd.acl.server.policy.PostgresService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.*;
import static iudx.apd.acl.server.notification.util.Constants.*;

public class DeleteNotification {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteNotification.class);
    private final PostgresService postgresService;
    private PgPool pool;

    public DeleteNotification(PostgresService postgresService) {
        this.postgresService = postgresService;
    }

    /**
     * Executes with draw consumer notification
     * @param notificationList list of notification ids to be deleted
     * @param user User object containing consumer details
     * @return failure or success response
     */
    public Future<JsonObject> initiateDeleteNotification(JsonArray notificationList, User user) {
        Promise<JsonObject> promise = Promise.promise();
        Set<UUID> requestIdSet = notificationList
                .stream()
                .map(val -> UUID.fromString(JsonObject.mapFrom(val).getString("id")))
                .collect(Collectors.toSet());
        boolean areThereDuplicateRequests = notificationList.size() != requestIdSet.size();
        if (areThereDuplicateRequests) {
            LOG.error("Duplicate notification ids present in the request");
            return Future.failedFuture(getFailureResponse(new JsonObject(), "Duplicate notification ids in the request"));
        }

        UUID[] requestUuid = requestIdSet.toArray(UUID[]::new);

        executeCheckOwnership(user, OWNERSHIP_CHECK_QUERY, requestUuid).onComplete(ownershipHandler -> {
            if (ownershipHandler.succeeded()) {
                /* response is all the notifications not owned by the user */
                boolean isOwner = ownershipHandler.result().getJsonArray(RESULT).isEmpty();
                if (isOwner) {
                    /*getting all the notifications that were previously withdrawn*/
                    executeGetNonPendingRequests(user, GET_WITHDRAWN_REQUESTS_QUERY, requestUuid).onComplete(withdrawnRequestsHandler -> {
                        if (withdrawnRequestsHandler.succeeded()) {
                            /* response is all the notifications that are already withdrawn and are sent in the request */
                            if (withdrawnRequestsHandler.result().getJsonArray(RESULT).isEmpty()) {
                                /* get the notification id in the request that is absent in the table */
                                executeGetAbsentNotificationId(GET_ID_NOT_FOUND_QUERY, requestUuid[0]).onComplete(absentNotificationHandler -> {
                                    if (absentNotificationHandler.succeeded()) {
                                        /* with draw the notification */
                                        executeWithDrawNotification(WITHDRAW_REQUEST, requestUuid).onComplete(withdrawNotificationHandler -> {
                                            if (withdrawNotificationHandler.succeeded()) {
                                                LOG.info("Withdraw notification successful :{}", withdrawNotificationHandler.result());
                                                executeFetchDeletedNotification(GET_WITHDRAWN_REQUEST, requestUuid).onComplete(fetchNotificationHandler -> {
                                                    if (fetchNotificationHandler.succeeded()) {
                                                        JsonObject response = fetchNotificationHandler.result();
                                                        response.put(STATUS_CODE, 200);
                                                        promise.complete(response);
                                                    } else {
                                                        LOG.error("Failure from fetchNotificationHandler : {}", fetchNotificationHandler.cause().getMessage());
                                                        promise.fail(fetchNotificationHandler.cause().getMessage());
                                                    }
                                                });
                                            } else {
                                                LOG.error("Failure from withdrawNotificationHandler : {}", withdrawNotificationHandler.cause().getMessage());
                                                promise.fail(withdrawNotificationHandler.cause().getMessage());
                                            }
                                        });
                                    } else {
                                        LOG.error("Failure in absentNotificationHandler {}", absentNotificationHandler.cause().getMessage());
                                        promise.fail(absentNotificationHandler.cause().getMessage());
                                    }
                                });

                            } else {
                                JsonObject response = new JsonObject();
                                response.put(TYPE, BAD_REQUEST.getValue());
                                response.put(TITLE, BAD_REQUEST.getUrn());
                                response.put(DETAIL, "Notifications can't be deleted as, the following notifications are withdrawn : " + getIds(withdrawnRequestsHandler));
                                promise.fail(response.encode());
                            }
                        } else {
                            LOG.error("Failure from withdrawnRequestsHandler : {}", withdrawnRequestsHandler.cause().getMessage());
                            promise.fail(withdrawnRequestsHandler.cause().getMessage());
                        }
                    });
                } else {
                    /* response contains all the notifications sent in the request that are not owned by the user */
                    JsonObject response = new JsonObject();
                    response.put(TYPE, FORBIDDEN.getValue());
                    response.put(TITLE, FORBIDDEN.getUrn());
                    response.put(DETAIL, "The following notifications don't belong to the user : " + getIds(ownershipHandler));
                    promise.fail(response.encode());
                }
            } else {
                LOG.error("Failure from ownershipHandler : {}", ownershipHandler.cause().getMessage());
                promise.fail(ownershipHandler.cause().getMessage());
            }
        });


        return promise.future();
    }

    /**
     * Fetches ids from the handler result
     * @param handler instance of AsyncResult of type JsonObject
     * @return set of notification ids
     */
    public Set<String> getIds(AsyncResult<JsonObject> handler) {
        return handler.result().getJsonArray(RESULT).stream()
                .map(value -> JsonObject.mapFrom(value).getString("requestId"))
                .collect(Collectors.toSet());
    }

    /**
     * Checks if the notification is for the given user
     * @param user User object containing consumer details
     * @param query to be executed
     * @param requestIds or notification ids in the request
     * @return request Ids not belonging to the user
     */
    public Future<JsonObject> executeCheckOwnership(User user, String query, UUID[] requestIds) {
        LOG.trace("inside executeCheckOwnership");
        Promise<JsonObject> promise = Promise.promise();
        Tuple tuple = Tuple.of(requestIds, user.getUserId());
        executeQuery(query, tuple, handler -> {
            if (handler.succeeded()) {
                LOG.info("Success {}", handler.result());
                promise.complete(handler.result());
            } else {
                LOG.error("Failed {}", handler.cause().getMessage());
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }

    /**
     * Fetches all the request Ids or notification ids that are already withdrawn
     * @param user User object containing consumer details
     * @param query to be executed
     * @param requestIds or notification ids in the request
     * @return with drawn notification ids as key-value pair in JsonObject
     */
    public Future<JsonObject> executeGetNonPendingRequests(User user, String query, UUID[] requestIds) {
        LOG.trace("inside executeGetWithDrawnRequests");
        Promise<JsonObject> promise = Promise.promise();
        Tuple tuple = Tuple.of(requestIds, user.getUserId());
        executeQuery(query, tuple, handler -> {
            if (handler.succeeded()) {
                LOG.info("Success {}", handler.result());
                promise.complete(handler.result());
            } else {
                LOG.error("Failed {}", handler.cause().getMessage());
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }


    /**
     * Get the notification id that is not present in the records
     * @param query to be executed
     * @param requestId or notification id in the request
     * @return a request Id that is absent from the database table
     */
    public Future<Boolean> executeGetAbsentNotificationId( String query, UUID requestId) {
        LOG.trace("inside executeGetNotifications");
        Promise<Boolean> promise = Promise.promise();
        Tuple tuple = Tuple.of(requestId);
        executeQuery(query, tuple, handler -> {
            if (handler.succeeded()) {
                /* result returned by the handler are the ids not present in the table but sent in the request*/
                boolean isResultEmpty = handler.result().getJsonArray(RESULT).isEmpty();
                LOG.info("id not present is : " + handler.result().getJsonArray(RESULT));
                if (!isResultEmpty) {
                    /* notification id not present */
                    JsonObject response = new JsonObject();
                    response.put(TYPE, NOT_FOUND.getValue());
                    response.put(TITLE, NOT_FOUND.getUrn());
                    response.put(DETAIL, "The following notification id is not found : " + getIds(handler));
                    promise.fail(response.encode());
                } else {
                    promise.complete(true);
                }
            } else {
                LOG.info("Failed : {}", handler.cause().getMessage());
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }

    /**
     * Withdraw notification id by setting the status as 'WITHDRAWN'
     * @param query to be executed
     * @param requestIds or notification ids in the request
     * @return true if the notifications are withdrawn or failure returned from the database
     */
    public Future<Boolean> executeWithDrawNotification(String query, UUID[] requestIds) {
        LOG.trace("inside executeWithDrawNotification");
        Promise<Boolean> promise = Promise.promise();
        Tuple tuple = Tuple.of(requestIds);
        executeQuery(query, tuple, handler -> {
            if (handler.succeeded()) {
                promise.complete(true);
            } else {
                LOG.info("Failed : {}", handler.cause().getMessage());
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }

    /**
     * Gets all the details about the withdrawn notification
     * @param query to be executed
     * @param requestIds or notification ids in the request
     * @return withdrawn notification details as an instance of Future JsonObject
     */
    public Future<JsonObject> executeFetchDeletedNotification(String query, UUID[] requestIds) {
        LOG.trace("inside executeWithDrawNotification");
        Promise<JsonObject> promise = Promise.promise();
        Tuple tuple = Tuple.of(requestIds);
        executeQuery(query, tuple, handler -> {
            if (handler.succeeded()) {
                LOG.info("Success : {}", handler.result());
                JsonObject jsonObject = handler.result();
                jsonObject.put(STATUS_CODE, HttpStatusCode.SUCCESS.getValue());
                promise.complete(jsonObject);
            } else {
                LOG.info("Failed : {}", handler.cause().getMessage());
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }

    /**
     * Executes the query by getting the Pgpool instance from postgres
     * @param query to be executes
     * @param tuple exchangeable values to be added in the query
     * @param handler
     */
    private void executeQuery(String query, Tuple tuple, Handler<AsyncResult<JsonObject>> handler) {

        pool = postgresService.getPool();
        Collector<Row, ?, List<JsonObject>> rowListCollector =
                Collectors.mapping(row -> row.toJson(), Collectors.toList());

        pool.withConnection(
                        sqlConnection ->
                                sqlConnection
                                        .preparedQuery(query)
                                        .collecting(rowListCollector)
                                        .execute(tuple)
                                        .map(rows -> rows.value()))
                .onSuccess(
                        successHandler -> {
                            JsonArray response = new JsonArray(successHandler);
                            JsonObject responseJson =
                                    new JsonObject()
                                            .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                                            .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                                            .put(RESULT, response);
                            handler.handle(Future.succeededFuture(responseJson));
                        })
                .onFailure(
                        failureHandler -> {
                            LOG.error("Failure while executing the query : {}", failureHandler.getMessage());
                            JsonObject response =
                                    new JsonObject()
                                            .put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
                                            .put(TITLE, ResponseUrn.DB_ERROR_URN.getMessage())
                                            .put(DETAIL, "Failure while executing query");
                            handler.handle(Future.failedFuture(response.encode()));
                        });
    }

    /**
     * Returns failure response that is to be shown to the client
     * @param response JsonObject instance to which failure response is added
     * @param detail failure message
     * @return failure response as String
     */
    private String getFailureResponse(JsonObject response, String detail) {
        return response
                .put(TYPE, BAD_REQUEST.getValue())
                .put(TITLE, BAD_REQUEST.getUrn())
                .put(DETAIL, detail)
                .encode();
    }
}
