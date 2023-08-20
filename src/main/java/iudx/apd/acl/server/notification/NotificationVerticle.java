package iudx.apd.acl.server.notification;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.apd.acl.server.policy.CatalogueClient;
import iudx.apd.acl.server.policy.PostgresService;

import static iudx.apd.acl.server.common.Constants.NOTIFICATION_SERVICE_ADDRESS;

public class NotificationVerticle extends AbstractVerticle {
    private DeleteNotification deleteNotification;
    private UpdateNotification updateNotification;
    private PostgresService postgresService;
    private NotificationServiceImpl notificationService;
    private CreateNotification createNotification;
    private CatalogueClient catalogueClient;

    @Override
    public void start() {
        postgresService = new PostgresService(config(), vertx);
        catalogueClient = new CatalogueClient(config());
        createNotification = new CreateNotification(postgresService, catalogueClient);
        deleteNotification = new DeleteNotification(postgresService);
        updateNotification = new UpdateNotification(postgresService);
        notificationService = new NotificationServiceImpl(deleteNotification, updateNotification, createNotification);
        new ServiceBinder(vertx).setAddress(NOTIFICATION_SERVICE_ADDRESS).register(NotificationService.class, notificationService);
    }
}
