package iudx.apd.acl.server.policy;

import static iudx.apd.acl.server.apiserver.util.Constants.*;
import static iudx.apd.acl.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.apd.acl.server.policy.util.Constants.*;

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
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetPolicy {
  private static final Logger LOG = LoggerFactory.getLogger(GetPolicy.class);
  private static final String FAILURE_MESSAGE = "Policy could not be fetched";
  private final PostgresService postgresService;
  private PgPool pool;
  private JsonObject response;

  public GetPolicy(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  public Future<JsonObject> initiateGetPolicy(User user) {
    return null;
  }
}