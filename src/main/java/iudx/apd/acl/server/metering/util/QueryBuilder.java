package iudx.apd.acl.server.metering.util;

import static iudx.apd.acl.server.metering.util.Constant.ORIGIN;
import static iudx.apd.acl.server.metering.util.Constant.ORIGIN_SERVER;
import static iudx.apd.acl.server.metering.util.Constant.PRIMARY_KEY;

import io.vertx.core.json.JsonObject;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryBuilder {
  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

  public JsonObject buildMessageForRmq(JsonObject request) {
    String primaryKey = UUID.randomUUID().toString().replace("-", "");
    request.put(PRIMARY_KEY, primaryKey);
    request.put(ORIGIN, ORIGIN_SERVER);

    LOGGER.debug("Info: Request " + request);
    return request;
  }
}

