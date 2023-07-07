package iudx.apd.acl.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import iudx.apd.acl.server.apiserver.response.RestResponse;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PostgresServiceImpl implements PostgresService {

  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);

  private final PgPool client;

  public PostgresServiceImpl(final PgPool pgclient) {
    this.client = pgclient;
  }

  @Override
  public PostgresService executeQuery(
      final String query, Handler<AsyncResult<JsonObject>> handler) {
    this.client
        .getConnection()
        .onFailure(
            failureHandler -> {
              LOGGER.error("Failed to get connection : " + failureHandler.getMessage());
              RestResponse response =
                  new RestResponse.Builder()
                      .build(
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(),
                          ResponseUrn.DB_ERROR_URN.getUrn(),
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription(),
                          failureHandler.getMessage());
              handler.handle(Future.failedFuture(response.toJson().encode()));
              failureHandler.printStackTrace();
            })
        .onSuccess(
            connectionHandler -> {
              connectionHandler
                  .query(query)
                  .execute()
                  .onFailure(
                      failedQueryHandler -> {
                        LOGGER.error(
                            "Failure while executing the query : "
                                + failedQueryHandler.getMessage());
                        RestResponse response =
                            new RestResponse.Builder()
                                .build(
                                    HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(),
                                    ResponseUrn.DB_ERROR_URN.getUrn(),
                                    HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription(),
                                    failedQueryHandler.getMessage());
                        handler.handle(Future.failedFuture(response.toJson().encode()));
                        connectionHandler.close();
                      })
                  .onSuccess(
                      rows -> {
                        LOGGER.info("Successfully executed query");
                          int count = 0;
                          for (Row row : rows) {
                          // some logic
                            count = row.getInteger("count");
                        }
                        JsonObject response =
                            new JsonObject()
                                    .put("result", "Successfully executed query")
                                    .put("count",count);
                        handler.handle(Future.succeededFuture(response));
                        connectionHandler.close();
                      });
            });
    return this;
  }

  @Override
  public PostgresService executeDbQuery(String query, Handler<AsyncResult<JsonObject>> handler) {
    this.client
        .getConnection()
        .onFailure(
            failureHandler -> {
              LOGGER.error("Failed to get connection : " + failureHandler.getMessage());
              RestResponse response =
                  new RestResponse.Builder()
                      .build(
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(),
                          ResponseUrn.DB_ERROR_URN.getUrn(),
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription(),
                          failureHandler.getMessage());
              handler.handle(Future.failedFuture(response.toJson().encode()));
              failureHandler.printStackTrace();
            })
        .onSuccess(
            connectionHandler -> {
              connectionHandler
                  .query(query)
                  .execute()
                  .onFailure(
                      failedQueryHandler -> {
                        LOGGER.error(
                            "Failure while executing the query : "
                                + failedQueryHandler.getMessage());
                        RestResponse response =
                            new RestResponse.Builder()
                                .build(
                                    HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(),
                                    ResponseUrn.DB_ERROR_URN.getUrn(),
                                    HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription(),
                                    failedQueryHandler.getMessage());
                        handler.handle(Future.failedFuture(response.toJson().encode()));
                        connectionHandler.close();
                      })
                  .onSuccess(
                      succeedQueryHandler -> {
                        LOGGER.info("Successfully executed query");
                        JsonObject response =
                            new JsonObject().put("result", "Successfully executed query");
                        handler.handle(Future.succeededFuture(response));
                        connectionHandler.close();
                      });
            });
    return this;
  }
}
