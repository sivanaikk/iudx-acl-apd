package iudx.apd.acl.server.notification;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.*;
import static iudx.apd.acl.server.notification.util.Constants.GET_REQUEST;
import static iudx.apd.acl.server.notification.util.Constants.WITHDRAW_REQUEST;

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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteNotification {
  private static final Logger LOG = LoggerFactory.getLogger(DeleteNotification.class);
  private final PostgresService postgresService;
  private PgPool pool;

  public DeleteNotification(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  /**
   * Executes with draw consumer notification
   *
   * @param notification request id to be deleted
   * @param user User object containing consumer details
   * @return failure or success response
   */
  public Future<JsonObject> initiateDeleteNotification(JsonObject notification, User user) {
    UUID requestUuid = UUID.fromString(notification.getString("id"));
    Future<Boolean> verifyFuture = verifyRequest(GET_REQUEST, requestUuid, user);

    Future<Boolean> withDrawNotificationFuture =
        verifyFuture.compose(
            isNotificationVerified -> {
              if (isNotificationVerified) {
                return executeWithDrawNotification(WITHDRAW_REQUEST, requestUuid);
              }
              return Future.failedFuture(verifyFuture.cause().getMessage());
            });
    return withDrawNotificationFuture.compose(
        isWithDrawNotificationSuccessful -> {
          if (isWithDrawNotificationSuccessful) {
            JsonObject response =
                new JsonObject()
                    .put(TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                    .put(TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                    .put(RESULT, "Request deleted successfully")
                    .put(STATUS_CODE, SUCCESS.getValue());
            return Future.succeededFuture(response);
          }
          return Future.failedFuture(withDrawNotificationFuture.cause().getMessage());
        });
  }

  /**
   * Checks if the request ID that is about to be withdrawn, belongs to the user and is not in
   * Pending status
   *
   * @param query to obtain the details about the notification
   * @param requestUuid notification id of the request in the form of UUID
   * @param user Instance of a user object
   * @return true if passes the given checks, if sends failure response accordingly
   */
  public Future<Boolean> verifyRequest(String query, UUID requestUuid, User user) {
    LOG.trace("inside verifyRequest method");
    Promise<Boolean> promise = Promise.promise();
    Tuple tuple = Tuple.of(requestUuid);
    String consumer = user.getUserId();
    LOG.trace("The consumer who's requesting to with draw notification is {}", consumer);

    executeQuery(
        query,
        tuple,
        handler -> {
          if (handler.succeeded()) {
            /* if the response is empty, notification is not found */
            if (handler.result().getJsonArray(RESULT).isEmpty()) {
              JsonObject response = new JsonObject();
              response.put(TYPE, NOT_FOUND.getValue());
              response.put(TITLE, ResponseUrn.RESOURCE_NOT_FOUND_URN.getUrn());
              response.put(DETAIL, "Request could not be withdrawn, as it is not found");
              promise.fail(response.encode());
            } else {
              JsonObject response = handler.result().getJsonArray(RESULT).getJsonObject(0);
              String consumerId = response.getString("user_id");
              String status = response.getString("status");
              /* ownership check */
              if (consumerId.equals(consumer)) {
                /* check if status is in pending state */
                if (status.equals("PENDING")) {
                  promise.complete(true);
                } else {
                  JsonObject failureResponse = new JsonObject();
                  failureResponse.put(TYPE, BAD_REQUEST.getValue());
                  failureResponse.put(TITLE, BAD_REQUEST.getUrn());
                  failureResponse.put(
                      DETAIL, "Request could not be withdrawn, as it is not in pending status");
                  promise.fail(failureResponse.encode());
                }
              } else {
                JsonObject failureResponse = new JsonObject();
                failureResponse.put(TYPE, FORBIDDEN.getValue());
                failureResponse.put(TITLE, FORBIDDEN.getUrn());
                failureResponse.put(
                    DETAIL, "Request could not be withdrawn, as it doesn't belong to the user");
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
   * Withdraw notification id by setting the status as 'WITHDRAWN'
   *
   * @param query to be executed
   * @param requestId or notification ids in the request
   * @return true if the notifications are withdrawn or failure returned from the database
   */
  public Future<Boolean> executeWithDrawNotification(String query, UUID requestId) {
    LOG.trace("inside executeWithDrawNotification method");
    Promise<Boolean> promise = Promise.promise();
    Tuple tuple = Tuple.of(requestId);
    executeQuery(
        query,
        tuple,
        handler -> {
          if (handler.succeeded()) {
            if (!handler.result().getJsonArray(RESULT).isEmpty()) {
              promise.complete(true);
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
   * @param query to be executes
   * @param tuple exchangeable values to be added in the query
   * @param handler AsyncResult JsonObject handler
   */
  public void executeQuery(String query, Tuple tuple, Handler<AsyncResult<JsonObject>> handler) {

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
                      .put(TITLE, ResponseUrn.DB_ERROR_URN.getUrn())
                      .put(DETAIL, "Failure while executing query");
              handler.handle(Future.failedFuture(response.encode()));
            });
  }
}
