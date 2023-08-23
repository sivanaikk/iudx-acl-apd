package iudx.apd.acl.server.notification;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.apd.acl.server.apiserver.util.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationServiceImpl implements NotificationService{
    private final DeleteNotification deleteNotification;
    private final UpdateNotification updateNotification;
    private final CreateNotification createNotification;

    private final GetNotification getNotification;

    public NotificationServiceImpl(DeleteNotification deleteNotification, UpdateNotification updateNotification, GetNotification getNotification, CreateNotification createNotification)
    {
        this.deleteNotification = deleteNotification;
        this.updateNotification = updateNotification;
        this.getNotification = getNotification;
        this.createNotification = createNotification;
    }
    private static final Logger LOG = LoggerFactory.getLogger(NotificationServiceImpl.class);
    @Override
    public Future<JsonObject> createNotification(JsonObject request, User user) {
        Promise<JsonObject> promise = Promise.promise();
        createNotification.initiateCreateNotification(request, user)
                .onComplete(handler -> {
                    if(handler.succeeded()){
                        LOG.info("Successfully created notification");
                        promise.complete(handler.result());
                    }else {
                        LOG.error("Failed to create notification");
                        promise.fail(handler.cause().getMessage());
                    }
                });
        return promise.future();
    }

    @Override
    public Future<JsonObject> deleteNotification(JsonObject notification, User user) {
        Promise<JsonObject> promise = Promise.promise();
        deleteNotification.initiateDeleteNotification(notification,user)
                .onComplete(
                        handler -> {
                            if (handler.succeeded()) {
                                LOG.info("Successfully deleted the notification");
                                promise.complete(handler.result());
                            } else {
                                LOG.error("Failed to delete the notification");
                                promise.fail(handler.cause().getMessage());
                            }
                        });
        return promise.future();
    }

    @Override
    public Future<JsonObject> getNotification(User user) {
        Promise<JsonObject> promise = Promise.promise();
        getNotification.initiateGetNotifications(user)
                .onComplete(
                        handler -> {
                            if (handler.succeeded()) {
                                LOG.info("Successfully fetched notification(s)");
                                promise.complete(handler.result());
                            } else {
                                LOG.error("Failed to fetch notification");
                                promise.fail(handler.cause().getMessage());
                            }
                        });
        return promise.future();
    }

    @Override
    public Future<JsonObject> updateNotification(JsonObject request, User user) {
        Promise<JsonObject> promise = Promise.promise();
        updateNotification.initiateUpdateNotification(request, user).onComplete(handler -> {
            if(handler.succeeded()){
                LOG.info("Successfully updated the notification");
                promise.complete(handler.result());
            }
            else
            {
                LOG.error("Failed to updated the notification");
                promise.fail(handler.cause().getMessage());
            }
        });
        return promise.future();
    }
}
