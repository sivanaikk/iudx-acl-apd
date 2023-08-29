package iudx.apd.acl.server.metering;

import static iudx.apd.acl.server.metering.util.Constant.EXCHANGE_NAME;
import static iudx.apd.acl.server.metering.util.Constant.ROUTING_KEY;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.apd.acl.server.metering.databroker.DataBrokerService;
import iudx.apd.acl.server.metering.util.QueryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringServiceImpl implements MeteringService {
  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
  private DataBrokerService dataBrokerService;
  private QueryBuilder queryBuilder = new QueryBuilder();

  public MeteringServiceImpl(DataBrokerService dataBrokerService) {
    this.dataBrokerService = dataBrokerService;
  }

  /**
   * @param request
   * @return
   */
  @Override
  public Future<Void> insertMeteringValuesInRmq(JsonObject request) {
    Promise<Void> promise = Promise.promise();
    JsonObject writeMessage = queryBuilder.buildMessageForRmq(request);
    LOGGER.info("write message =  {}", writeMessage);
    dataBrokerService
        .publishMessage(EXCHANGE_NAME, ROUTING_KEY, writeMessage)
        .onSuccess(
            successHandler -> {
              LOGGER.info("Audit data Published into rmq");
              promise.complete();
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(failureHandler.getMessage());
              LOGGER.debug("Fail to Published audit data into rmq");
              promise.fail(failureHandler.getMessage());
            });

    return promise.future();
  }
}
