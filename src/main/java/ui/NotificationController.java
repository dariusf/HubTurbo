package ui;

import javafx.application.Platform;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;
import ui.components.Notification;
import util.TickingTimer;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class NotificationController {

    private NotificationPane notificationPane;
    private TickingTimer notificationPaneTimer;
    private Optional<Notification> notification = Optional.empty();

    public NotificationController(NotificationPane notificationPane) {
        this.notificationPane = notificationPane;
    }

    void showNotification(Notification notification) {
        Platform.runLater(() -> {
            if (notificationPane.isShowing() && this.notification.isPresent()) {
                hideNotification();
            }
            notificationPane.setGraphic(notification.getIcon());
            notificationPane.setText(notification.getMessage());
            notificationPane.getActions().clear();
            notificationPane.getActions().add(new Action(notification.getButtonLabel(),
                    actionEvent -> {
                        notification.getButtonRunnable().run();
                        hideNotification();
                    }));
            notificationPaneTimer = new TickingTimer("Notification Timer", notification.getTimeoutDuration(),
                    integer -> {}, () -> Platform.runLater(this::triggerTimeoutAction), TimeUnit.SECONDS);
            notificationPane.show();
            notificationPaneTimer.start();
            this.notification = Optional.of(notification);
        });
    }

    private void triggerTimeoutAction() {
        // must be run in a Platform.runLater
        if (notificationPane.isShowing() && notification.isPresent()) {
            if (notification.get().getNotificationType() == Notification.NotificationType.ACTIONONBUTTONANDTIMEOUT) {
                notification.get().getTimeoutRunnable().run();
            }
            // other NotifcationTypes can be implemented here if needed
            hideNotification();
        }
    }

    private void hideNotification() {
        // must be run in a Platform.runLater
        notificationPaneTimer.stop();
        notificationPane.hide();
        notification = Optional.empty();
    }

    void triggerNotificationAction() {
        Platform.runLater(() -> {
            if (notificationPane.isShowing() && notification.isPresent()) {
                if (notification.get().getNotificationType() ==
                        Notification.NotificationType.ACTIONONBUTTONANDTIMEOUT) {
                    notification.get().getButtonRunnable().run();
                }
                // other NotifcationTypes can be implemented here if needed
                hideNotification();
            }
        });
    }

}
