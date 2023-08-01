package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.policy.util.Constants.DB_RECONNECT_ATTEMPTS;
import static iudx.apd.acl.server.policy.util.Constants.DB_RECONNECT_INTERVAL_MS;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;


public class PostgresService {
  private final PgPool pool;

  public PostgresService(JsonObject config, Vertx vertx) {
    /* Database Properties */
    String databaseIP = config.getString("databaseIP");
    int databasePort = config.getInteger("databasePort");
    String databaseName = config.getString("databaseName");
    String databaseUserName = config.getString("databaseUserName");
    String databasePassword = config.getString("databasePassword");
    int poolSize = config.getInteger("poolSize");

    /* Set Connection Object and schema */
    PgConnectOptions connectOptions =
      new PgConnectOptions().setPort(databasePort).setHost(databaseIP).setDatabase(databaseName)
        .setUser(databaseUserName).setPassword(databasePassword)
        .setReconnectAttempts(DB_RECONNECT_ATTEMPTS)
        .setReconnectInterval(DB_RECONNECT_INTERVAL_MS);

    /* Pool options */
    PoolOptions poolOptions = new PoolOptions().setMaxSize(poolSize);

    /* Create the client pool */
    this.pool = PgPool.pool(vertx, connectOptions, poolOptions);
  }
  public PgPool getPool() {
    return pool;
  }
}