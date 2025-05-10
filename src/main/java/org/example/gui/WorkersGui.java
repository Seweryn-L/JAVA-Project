package org.example.gui;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.example.model.ConveyorBelt;
import org.example.app.AppMain;

/**
 * Class responsible for displaying worker notifications in the GUI
 */
public class WorkersGui {

    private Stage stage;
    private final ConveyorBelt belt = AppMain.belt;

    // Track last notification timestamps to prevent spam
    private long lastP1NotificationTime = 0;
    private long lastP2NotificationTime = 0;
    private long lastP3NotificationTime = 0;

    // Minimum time between notifications (milliseconds)
    private static final long NOTIFICATION_COOLDOWN = 500;

    public WorkersGui(Stage stage) {
        this.stage = stage;
    }

    /**
     * Shows a notification for a worker when they add bricks to the conveyor belt
     *
     * @param status Worker's status (true when actively adding bricks)
     * @param workerName Name of the worker (P1, P2, P3)
     */
    public void showNotification(boolean status, String workerName) {
        // Skip if status is false - we only show notifications on TRUE events
        if (!status) {
            return;
        }

        // Check if the stage is valid - this must run on UI thread
        Platform.runLater(() -> {
            try {
                // Stop if stage is not ready yet
                if (stage == null || stage.getScene() == null) {
                    System.err.println("Stage not ready for notifications");
                    return;
                }

                // Check if time cooldown has passed to prevent notification spam
                long currentTime = System.currentTimeMillis();
                boolean canShowNotification = false;

                switch (workerName) {
                    case "P1":
                        if (currentTime - lastP1NotificationTime > NOTIFICATION_COOLDOWN) {
                            lastP1NotificationTime = currentTime;
                            canShowNotification = true;
                        }
                        break;
                    case "P2":
                        if (currentTime - lastP2NotificationTime > NOTIFICATION_COOLDOWN) {
                            lastP2NotificationTime = currentTime;
                            canShowNotification = true;
                        }
                        break;
                    case "P3":
                        if (currentTime - lastP3NotificationTime > NOTIFICATION_COOLDOWN) {
                            lastP3NotificationTime = currentTime;
                            canShowNotification = true;
                        }
                        break;
                }

                if (!canShowNotification) {
                    return;
                }

                // Prepare notification message and position based on worker
                String bricks;
                String message;
                int x = 0, y = 0;

                switch (workerName) {
                    case "P1":
                        bricks = "\uD83E\uDDF1"; // one brick emoji
                        message = workerName + " + " + bricks;
                        x = -400; y = -80;
                        break;
                    case "P2":
                        bricks = "\uD83E\uDDF1\uD83E\uDDF1"; // two brick emojis
                        message = workerName + " + " + bricks;
                        x = -280; y = -60;
                        break;
                    case "P3":
                        bricks = "\uD83E\uDDF1\uD83E\uDDF1\uD83E\uDDF1"; // three brick emojis
                        message = workerName + " + " + bricks;
                        x = -130; y = -80;
                        break;
                    default:
                        bricks = "\uD83E\uDDF1";
                        message = workerName + " + " + bricks;
                        x = 0; y = 0;
                }


                // Show the notification directly, bypass the full check
                showDirectNotification(message, x, y, workerName);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Directly shows a notification without additional checks
     *
     * @param message Message to display
     * @param x X position
     * @param y Y position
     */
    private void showDirectNotification(String message, int x, int y, String workerName) {
        try {
            // Add a debug toast to scene first - this is important to make it more reliable
            directToast(message, x, y, workerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows a toast notification directly embedded in the stage
     * This bypasses the CustomToast class that might be causing issues
     */
    private void directToast(String message, double x, double y, String workerName) {
        try {
            if (stage == null || stage.getScene() == null) {
                return;
            }

            // Use CustomToast with explicit error handling
            CustomToast.show(stage, message, x, y, 1500, workerName);

        } catch (Exception e) {
            System.err.println("Error in directToast: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates the stage reference
     */
    public void updateStage(Stage stage) {
        this.stage = stage;

    }
}