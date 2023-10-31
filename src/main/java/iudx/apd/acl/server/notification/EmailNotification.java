package iudx.apd.acl.server.notification;

import static iudx.apd.acl.server.notification.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.*;
import iudx.apd.acl.server.apiserver.util.Role;
import iudx.apd.acl.server.apiserver.util.User;
import java.util.ArrayList;
import java.util.List;
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
  private final boolean notifyByEmail;
  private final GetDelegateEmailIds getDelegateEmailIds;
  MailClient mailClient;
  private List<String> supportEmailIds;
  private List<String> delegateEmailIds;
  private boolean hasFetchDelegateFailed;

  public EmailNotification(
      Vertx vertx, JsonObject config, GetDelegateEmailIds getDelegateEmailIds) {
    this.emailHostname = config.getString("emailHostName");
    this.emailPort = config.getInteger("emailPort");
    this.emailUserName = config.getString("emailUserName");
    this.emailPassword = config.getString("emailPassword");
    this.senderEmail = config.getString("emailSender");
    this.supportEmail = config.getJsonArray("emailSupport");
    this.supportEmailIds = supportEmail.getList();
    this.publisherPanelUrl = config.getString("publisherPanelUrl");
    this.notifyByEmail = config.getBoolean("notifyByEmail");
    this.senderName = config.getString("senderName");
    this.getDelegateEmailIds = getDelegateEmailIds;

    MailConfig mailConfig = new MailConfig();
    mailConfig.setStarttls(StartTLSOptions.REQUIRED);
    mailConfig.setLogin(LoginOption.REQUIRED);
    mailConfig.setKeepAliveTimeout(5);
    mailConfig.setHostname(emailHostname);
    mailConfig.setPort(emailPort);
    mailConfig.setUsername(emailUserName);
    mailConfig.setPassword(emailPassword);
    mailConfig.setAllowRcptErrors(true);

    if (mailClient == null) {
      this.mailClient = MailClient.create(vertx, mailConfig);
    }
  }

  public Future<Boolean> sendEmail(
      User consumer, User provider, String itemId, String resourceServerUrl) {
    final Promise<Boolean> promise = Promise.promise();

    if (!notifyByEmail) {
      return Future.succeededFuture(true);
    }
    final String consumerId = consumer.getUserId();
    final String consumerFirstName = consumer.getFirstName();
    final String consumerLastName = consumer.getLastName();
    final String consumerEmailId = consumer.getEmailId();

    final String providerEmailId = provider.getEmailId();

    List<String> ccList = new ArrayList<>();
    /* add all the support emailIds to cc*/
    ccList.addAll(supportEmailIds);

    Future<JsonArray> getEmailIdFuture =
        getDelegateEmailIds.getEmails(
            provider.getUserId(), resourceServerUrl, Role.PROVIDER.getRole());
    getEmailIdFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            JsonArray jsonArray = handler.result();
            delegateEmailIds = jsonArray.getList();
            /* add all the delegate email Ids to cc*/
            ccList.addAll(delegateEmailIds);
          } else {
            LOGGER.error("Failure: {}", handler.cause().getMessage());
            hasFetchDelegateFailed = true;
            promise.fail("Failed to fetch delegate email Ids");
          }
        });

    if (hasFetchDelegateFailed) {
      return promise.future();
    }
    String body =
        HTML_EMAIL_BODY
            .replace("${CONSUMER_FIRST_NAME}", consumerFirstName)
            .replace("${CONSUMER_LAST_NAME}", consumerLastName)
            .replace("${CONSUMER_EMAIL_ID}", consumerEmailId)
            .replace("${PUBLISHER_PANEL_URL}", publisherPanelUrl)
            .replace("${SENDER'S_NAME}", senderName);

    MailMessage message = new MailMessage();
    message.setFrom(senderEmail);
    message.setTo(providerEmailId);
    message.setCc(ccList);
    message.setHtml(body);
    message.setSubject("Requesting for policy");

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
}
