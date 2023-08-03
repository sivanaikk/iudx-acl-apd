package iudx.apd.acl.server.notification;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.apd.acl.server.policy.PostgresService;

import static iudx.apd.acl.server.common.Constants.NOTIFICATION_SERVICE_ADDRESS;

public class NotificationVerticle extends AbstractVerticle {
    private DeleteNotification deleteNotification;
    private UpdateNotification updateNotification;
    private PostgresService postgresService;
    private NotificationServiceImpl notificationService;

    @Override
    public void start() {
        postgresService = new PostgresService(config(), vertx);
        deleteNotification = new DeleteNotification(postgresService);
        updateNotification = new UpdateNotification(postgresService);
        notificationService = new NotificationServiceImpl(deleteNotification, updateNotification);
        new ServiceBinder(vertx).setAddress(NOTIFICATION_SERVICE_ADDRESS).register(NotificationService.class, notificationService);
    }
}
