package org.example.gui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Utility class for displaying toast-like notifications on the UI
 */
public class CustomToast {
    private static final Duration FADE_DURATION = Duration.millis(300);

    /**
     * Shows a toast notification on the screen
     *
     * @param owner Owner Stage for the toast
     * @param message Message to display
     * @param offsetX X position offset
     * @param offsetY Y position offset
     * @param durationMillis Duration in milliseconds to show the toast
     */
    public static void show(Stage owner, String message, double offsetX, double offsetY, int durationMillis, String workerName) {
        // Make sure we're on the JavaFX thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(owner, message, offsetX, offsetY, durationMillis, workerName));
            return;
        }

        // Make sure we have a valid stage and scene
        if (owner == null) {
            System.err.println("Cannot show toast: owner is null");
            return;
        }

        if (owner.getScene() == null) {
            System.err.println("Cannot show toast: scene is null");
            return;
        }

        if (owner.getScene().getRoot() == null) {
            System.err.println("Cannot show toast: root is null");
            return;
        }

        try {
            // Safe cast - we know the root should be a Pane
            Pane rootPane;

            if (owner.getScene().getRoot() instanceof Pane) {
                rootPane = (Pane) owner.getScene().getRoot();
            } else {
                System.err.println("Root is not a Pane, it's: " + owner.getScene().getRoot().getClass().getName());
                // Fallback approach - create overlay
                rootPane = createToastOverlay(owner);
            }

            // Create the toast label
            Label toastLabel = new Label(message);
            switch (workerName) {
                case "P1" -> toastLabel.getStyleClass().add("custom-toast-label-p1");
                case "P2" -> toastLabel.getStyleClass().add("custom-toast-label-p2");
                case "P3" -> toastLabel.getStyleClass().add("custom-toast-label-p3");
                case null, default -> toastLabel.getStyleClass().add("custom-toast-label");
            }

            // Positioning
            toastLabel.setTranslateX(offsetX);
            toastLabel.setTranslateY(offsetY);

            // Add to GUI
            rootPane.getChildren().add(toastLabel);

            // Create fade-in animation
            FadeTransition fadeIn = new FadeTransition(FADE_DURATION, toastLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            // Create fade-out animation
            FadeTransition fadeOut = new FadeTransition(FADE_DURATION, toastLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            // Create pause between animations
            PauseTransition pause = new PauseTransition(Duration.millis(durationMillis));

            // Chain animations
            fadeIn.setOnFinished(e -> pause.play());
            pause.setOnFinished(e -> fadeOut.play());
            fadeOut.setOnFinished(e -> {
                // Make sure we're removing the label on the JavaFX thread
                Platform.runLater(() -> {
                    rootPane.getChildren().remove(toastLabel);
                });
            });

            // Start animation
            fadeIn.play();
        } catch (Exception e) {
            // Log exception but don't crash the app over a notification
            System.err.println("Error showing toast notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static StackPane createToastOverlay(Stage owner) {
        StackPane overlay = new StackPane();
        overlay.setPrefSize(owner.getWidth(), owner.getHeight());
        overlay.setMouseTransparent(true); // Don't block mouse events
        overlay.setStyle("-fx-background-color: transparent;");

        // Add overlay to scene
        if (owner.getScene().getRoot() instanceof Pane) {
            ((Pane) owner.getScene().getRoot()).getChildren().add(overlay);
        } else {
            System.err.println("Cannot add overlay: root is not a Pane");
            // Last resort - replace scene root with new container
            StackPane newRoot = new StackPane();
            newRoot.getChildren().addAll(owner.getScene().getRoot(), overlay);
            owner.getScene().setRoot(newRoot);
        }

        return overlay;
    }
}