package iudx.apd.acl.server.notification;

import static iudx.apd.acl.server.apiserver.util.Constants.EMAIL_OPTIONS;
import static iudx.apd.acl.server.common.Constants.NOTIFICATION_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.apd.acl.server.policy.CatalogueClient;
import iudx.apd.acl.server.policy.PostgresService;

public class NotificationVerticle extends AbstractVerticle {
  private DeleteNotification deleteNotification;
  private UpdateNotification updateNotification;
  private GetNotification getNotification;
  private PostgresService postgresService;
  private NotificationServiceImpl notificationService;
  private CreateNotification createNotification;
  private CatalogueClient catalogueClient;
  private EmailNotification emailNotification;

  @Override
  public void start() {
    postgresService = new PostgresService(config(), vertx);
    catalogueClient = new CatalogueClient(config());
    emailNotification = new EmailNotification(vertx, config().getJsonObject(EMAIL_OPTIONS));
    createNotification = new CreateNotification(postgresService, catalogueClient, emailNotification);
    deleteNotification = new DeleteNotification(postgresService);
    updateNotification = new UpdateNotification(postgresService);
    getNotification = new GetNotification(postgresService);
    notificationService =
        new NotificationServiceImpl(
            deleteNotification, updateNotification, getNotification, createNotification);
    new ServiceBinder(vertx)
        .setAddress(NOTIFICATION_SERVICE_ADDRESS)
        .register(NotificationService.class, notificationService);
  }
}
