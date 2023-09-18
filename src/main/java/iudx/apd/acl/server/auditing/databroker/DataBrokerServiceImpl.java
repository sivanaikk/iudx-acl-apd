package iudx.apd.acl.server.auditing.databroker;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataBrokerServiceImpl implements DataBrokerService {
  private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceImpl.class);
  private RabbitMQClient rabbitWebclient;

  public DataBrokerServiceImpl(RabbitMQClient rabbitWebclient) {
    this.rabbitWebclient = rabbitWebclient;
  }

  /**
   * @param toExchange Exchange name
   * @param routingKey routingKey
   * @param messageBody JsonObject
   * @return void future
   */
  @Override
  public Future<Void> publishMessage(String toExchange, String routingKey, JsonObject messageBody) {
    Promise<Void> promise = Promise.promise();

    Future<Void> rabbitMqClientStartFuture;
    Buffer buffer = Buffer.buffer(messageBody.toString());
    if (!rabbitWebclient.isConnected()) {
      rabbitMqClientStartFuture = rabbitWebclient.start();
    } else {
      rabbitMqClientStartFuture = Future.succeededFuture();
    }
    rabbitMqClientStartFuture
        .compose(
            rabbitStartupFuture -> rabbitWebclient.basicPublish(toExchange, routingKey, buffer))
        .onSuccess(
            successHandler -> {
              LOGGER.debug("Data Published into RMQ");
              promise.complete();
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(failureHandler.getMessage());
              promise.fail(failureHandler.getMessage());
            });

    return promise.future();
  }
}
