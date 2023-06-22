package iudx.apd.acl.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
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
    return null;
  }
}
