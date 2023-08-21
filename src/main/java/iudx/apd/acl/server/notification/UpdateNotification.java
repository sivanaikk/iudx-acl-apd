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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.apiserver.util.Constants.DETAIL;
import static iudx.apd.acl.server.common.HttpStatusCode.*;
import static iudx.apd.acl.server.common.HttpStatusCode.FORBIDDEN;
import static iudx.apd.acl.server.notification.util.Constants.GET_REQUEST;
import static iudx.apd.acl.server.notification.util.Constants.REJECT_NOTIFICATION;

public class UpdateNotification {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateNotification.class);
    public PgPool pool;
    private final PostgresService postgresService;

    public UpdateNotification(PostgresService postgresService) {
        this.postgresService = postgresService;

    }

    public Future<JsonObject> initiateUpdateNotification(JsonObject notification, User user) {
        LOG.trace("inside initiateUpdateNotification method");
        UUID notificationId = UUID.fromString(notification.getString("requestId"));

        Future<Boolean> checkNotificationFuture = checkValidityOfRequest(GET_REQUEST, notificationId, user);
        return checkNotificationFuture.compose(isNotificationValid -> {
            if (isNotificationValid) {
                return updateRejectedNotification(REJECT_NOTIFICATION, notificationId);
            }
            return Future.failedFuture(checkNotificationFuture.cause().getMessage());
        });
    }


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
                    response.put(TITLE, NOT_FOUND.getUrn());
                    response.put(DETAIL, "Request could not be rejected, as it is not found");
                    promise.fail(response.encode());
                } else {
                    JsonObject response = handler.result().getJsonArray(RESULT).getJsonObject(0);
                    String owner = response.getString("owner_id");
                    String status = response.getString("status");
                    /* ownership check */
                    if (owner.equals(ownerId)) {
                        /* check if status is in pending state */
                        if (status.equalsIgnoreCase("PENDING")) {
                            promise.complete(true);
                        } else {
                            JsonObject failureResponse = new JsonObject();
                            failureResponse.put(TYPE, BAD_REQUEST.getValue());
                            failureResponse.put(TITLE, BAD_REQUEST.getUrn());
                            failureResponse.put(DETAIL, "Request could not be rejected, as it is not in pending status");
                            promise.fail(failureResponse.encode());
                        }
                    } else {
                        JsonObject failureResponse = new JsonObject();
                        failureResponse.put(TYPE, FORBIDDEN.getValue());
                        failureResponse.put(TITLE, FORBIDDEN.getUrn());
                        failureResponse.put(DETAIL, "Request could not be rejected, as it is doesn't belong to the user");
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
                            .put(RESULT, "Request deleted successfully")
                            .put(STATUS_CODE, SUCCESS.getValue());
                    promise.complete(response);
                } else {
                    LOG.trace("Notification has expired ");
                    JsonObject failureResponse = new JsonObject();
                    failureResponse.put(TYPE, BAD_REQUEST.getValue());
                    failureResponse.put(TITLE, BAD_REQUEST.getUrn());
                    failureResponse.put(DETAIL, "Request could not be withdrawn, as it is expired");
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

        pool = postgresService.getPool();
        Collector<Row, ?, List<JsonObject>> rowListCollector = Collectors.mapping(row -> row.toJson(), Collectors.toList());

        pool.withConnection(sqlConnection -> sqlConnection.preparedQuery(query).collecting(rowListCollector).execute(tuple).map(rows -> rows.value())).onSuccess(successHandler -> {
            JsonArray response = new JsonArray(successHandler);
            JsonObject responseJson = new JsonObject().put(TYPE, ResponseUrn.SUCCESS_URN.getUrn()).put(TITLE, ResponseUrn.SUCCESS_URN.getMessage()).put(RESULT, response);
            handler.handle(Future.succeededFuture(responseJson));
        }).onFailure(failureHandler -> {
            LOG.error("Failure while executing the query : {}", failureHandler.getMessage());
            JsonObject response = new JsonObject().put(TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR.getValue()).put(TITLE, ResponseUrn.DB_ERROR_URN.getMessage()).put(DETAIL, "Failure while executing query");
            handler.handle(Future.failedFuture(response.encode()));
        });
    }

}
