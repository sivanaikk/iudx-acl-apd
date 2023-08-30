package iudx.apd.acl.server.notification;

import static iudx.apd.acl.server.notification.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.*;
import iudx.apd.acl.server.apiserver.util.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailNotification {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotification.class);
  private final String emailHostname;
  private final int emailPort;
  private final String emailUserName;
  private final String emailPassword;
  private final String senderEmail;
  private final String senderName;
  private final JsonArray supportEmail;
  private final String publisherPanelUrl;
  private final MailClient mailClient;
  private final boolean notifyByEmail;
  private List<String> supportEmailIds;

  public EmailNotification(Vertx vertx, JsonObject config) {
    this.emailHostname = config.getString("emailHostName");
    this.emailPort = config.getInteger("emailPort");
    this.emailUserName = config.getString("emailUserName");
    this.emailPassword = config.getString("emailPassword");
    this.senderEmail = config.getString("emailSender");
    this.supportEmail = config.getJsonArray("emailSupport");
    supportEmailIds = supportEmail.getList();
    this.publisherPanelUrl = config.getString("publisherPanelUrl");
    this.notifyByEmail = config.getBoolean("notifyByEmail");
    this.senderName = config.getString("senderName");

    MailConfig mailConfig = new MailConfig();
    mailConfig.setStarttls(StartTLSOptions.REQUIRED);
    mailConfig.setLogin(LoginOption.REQUIRED);
    mailConfig.setKeepAliveTimeout(5);
    mailConfig.setHostname(emailHostname);
    mailConfig.setPort(emailPort);
    mailConfig.setUsername(emailUserName);
    mailConfig.setPassword(emailPassword);
    mailConfig.setAllowRcptErrors(true);

    this.mailClient = MailClient.create(vertx, mailConfig);
  }

  public Future<Boolean> sendEmail(User consumer, User provider, String itemId) {
    final Promise<Boolean> promise = Promise.promise();

    if (!notifyByEmail) {
      return Future.succeededFuture(true);
    }
    String consumerId = consumer.getUserId();
    String consumerFirstName = consumer.getFirstName();
    String consumerLastName = consumer.getLastName();
    String consumerEmailId = consumer.getEmailId();

    String providerEmailId = provider.getEmailId();

    List<String> delegateEmailIds = getDelegateEmailIds();

    List<String> ccList = new ArrayList<>(delegateEmailIds);
    ccList.addAll(supportEmailIds);

    String body =
            HTML_EMAIL_BODY
            .replace("${CONSUMER_FIRST_NAME}", consumerFirstName)
            .replace("${CONSUMER_LAST_NAME}", consumerLastName)
            .replace("${CONSUMER_ID}", consumerId)
            .replace("${CONSUMER_EMAIL_ID}", consumerEmailId)
            .replace("${ITEM_ID}", itemId)
            .replace("${PUBLISHER_PANEL_URL}", publisherPanelUrl)
            .replace("${SENDER'S_NAME}", senderName);

    MailMessage message = new MailMessage();
    message.setFrom(senderEmail);
    message.setTo(providerEmailId);
    message.setCc(ccList);
    message.setHtml(body);
    message.setSubject("Requesting for policy : " + itemId);

    mailClient
        .sendMail(message)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("Email successfully sent: {}", handler.result());
                promise.complete(true);
              } else {
                LOGGER.error("Failure in sending email {}", handler.cause().getMessage());
                promise.fail(handler.cause().getMessage());
              }
            });

    return promise.future();
  }

  // TODO: Auth call to get the delegate emailIds
  public List<String> getDelegateEmailIds() {
    return List.of(generateRandomEmailId(), generateRandomEmailId());
  }

  public String generateRandomString() {
    return UUID.randomUUID().toString();
  }

  public String generateRandomEmailId() {
    return generateRandomString().substring(0, 6)
        + "@"
        + generateRandomString().substring(0, 3)
        + ".com";
  }
}
