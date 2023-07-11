package iudx.apd.acl.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.apd.acl.server.apiserver.response.RestResponse;
import iudx.apd.acl.server.common.HttpStatusCode;
import iudx.apd.acl.server.common.ResponseUrn;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PostgresServiceImpl implements PostgresService {

  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);

  private final PgPool client;

  public PostgresServiceImpl(final PgPool client) {
    this.client = client;
  }

  /**
   * Executes the SQL Query by connecting with postgreSQL using a PgPool client
   *
   * @param query SQL Query to execute
   * @param params params in the form of Vert.x Tuple required for executing query
   * @param handler AsyncResult JsonObject handler
   * @return PostgresService object
   */
  @Override
  public PostgresService executeQueryWithParams(
      String query, JsonObject params, Handler<AsyncResult<JsonObject>> handler) {
    Collector<Row, ?, List<JsonObject>> rowListCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());
    UUID[] policyUuid =
        params.stream()
            .map(e -> e.getValue())
            .map(s -> UUID.fromString((String) s))
            .toArray(UUID[]::new);
    Tuple tuple = Tuple.of(policyUuid);
    client
        .withConnection(
            sqlConnection ->
                sqlConnection
                    .preparedQuery(query)
                    .collecting(rowListCollector)
                    .execute(tuple)
                    .map(rows -> rows.value()))
        .onSuccess(
            successHandler -> {
              JsonArray response = new JsonArray(successHandler);
              JsonObject responseJson = new JsonObject().put("result", response);
              handler.handle(Future.succeededFuture(responseJson));
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("Failure while executing the query : " + failureHandler.getMessage());
              RestResponse response =
                  new RestResponse.Builder()
                      .build(
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getValue(),
                          ResponseUrn.DB_ERROR_URN.getUrn(),
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription(),
                          failureHandler.getMessage());
              handler.handle(Future.failedFuture(response.toJson().encode()));
            });
    return this;
  }

  /**
   * Executes SQL query by connecting with PostgreSQL using a PgPool client
   *
   * @param query SQL Query to be executed
   * @param handler AsyncResult JsonObject handler
   * @return PostgresService object
   */
  @Override
  public PostgresService executeQuery(String query, Handler<AsyncResult<JsonObject>> handler) {

    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());
    client
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
                  .collecting(rowCollector)
                  .execute()
                  .map(row -> row.value())
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
                      successHandler -> {
                        LOGGER.info("Successfully executed query");
                        handler.handle(
                            Future.succeededFuture(new JsonObject().put("result", successHandler)));
                        connectionHandler.close();
                      });
            });
    return this;
  }
}
