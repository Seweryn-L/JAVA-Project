package org.example.gui;

import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.Objects;

public class Background {
    private final AnchorPane root;
    private ImageView smallBrickImageView;
    private ImageView truckImageView;

    public Background() {

        // Ładowanie obrazów
        Image truckImage = loadImage("/truck.png");
        truckImageView = createImageView(truckImage, 400, true);

        Image conveyorBeltImage = loadImage("/conveyorBelt.png");
        ImageView conveyorBeltImageView = new ImageView(conveyorBeltImage);
        conveyorBeltImageView.setFitWidth(1200);
        conveyorBeltImageView.setFitHeight(150);
        conveyorBeltImageView.setPreserveRatio(true);

        Image worker1Image = loadImage("/worker1.png");
        ImageView worker1ImageView = createImageView(worker1Image, 100, true);

        Image worker2Image = loadImage("/worker2.png");
        ImageView worker2ImageView = createImageView(worker2Image, 100, true);

        Image worker3Image = loadImage("/worker3.png");
        ImageView worker3ImageView = createImageView(worker3Image, 125, true);

        Image brickImage = loadImage("/bricks.png");
        ImageView brickImageView = createImageView(brickImage, 300, true);

        Image smallBrickImage = loadImage("/bricks.png");
        smallBrickImageView = createImageView(smallBrickImage, 100, true);
        // Tworzenie głównego kontenera

        root = new AnchorPane(
                conveyorBeltImageView,
                smallBrickImageView,
                truckImageView,
                worker1ImageView,
                worker2ImageView,
                worker3ImageView,
                brickImageView
        );

        // Ustawienie tła
        setBackgroundImage();

        AnchorPane.setBottomAnchor(smallBrickImageView, 60.0);
        AnchorPane.setLeftAnchor(smallBrickImageView, 340.0);

        AnchorPane.setBottomAnchor(truckImageView, -30.0);
        AnchorPane.setRightAnchor(truckImageView, 20.0);

        AnchorPane.setBottomAnchor(conveyorBeltImageView, 0.0);
        AnchorPane.setLeftAnchor(conveyorBeltImageView, 300.0);

        AnchorPane.setBottomAnchor(worker1ImageView, 0.0);
        AnchorPane.setLeftAnchor(worker1ImageView, 10.0);

        AnchorPane.setBottomAnchor(worker2ImageView, 0.0);
        AnchorPane.setLeftAnchor(worker2ImageView, 130.0);

        AnchorPane.setBottomAnchor(worker3ImageView, 0.0);
        AnchorPane.setLeftAnchor(worker3ImageView, 250.0);

        AnchorPane.setBottomAnchor(brickImageView, -100.0);
        AnchorPane.setLeftAnchor(brickImageView, 10.0);
    }
    public void moveBrick() {
        TranslateTransition transition = new TranslateTransition();
        transition.setNode(smallBrickImageView);
        transition.setDuration(Duration.seconds(2.5));
        transition.setToX(200);
        transition.setCycleCount(1);
        transition.play();
        // Resetuj pozycję przed animacją
        transition.setOnFinished(e -> {
            smallBrickImageView.setTranslateX(0);
            smallBrickImageView.setTranslateY(0);
        });
    }

    public void moveTruck() {
        // Animacja w prawo
        TranslateTransition moveRight = new TranslateTransition(Duration.seconds(2.5), truckImageView);
        moveRight.setToX(400);

        // Animacja powrotu (w lewo)
        TranslateTransition moveLeft = new TranslateTransition(Duration.seconds(2.5), truckImageView);
        moveLeft.setToX(0);

        // Sekwencja animacji: najpierw w prawo, potem w lewo
        SequentialTransition sequence = new SequentialTransition(moveRight, moveLeft);
        sequence.play();
    }

    // Nowa metoda do animacji truckBox
    public void moveTruckBox(VBox truckBox) {
        // Animacja w prawo
        TranslateTransition moveRight = new TranslateTransition(Duration.seconds(2.5), truckBox);
        moveRight.setToX(400);

        // Animacja powrotu (w lewo)
        TranslateTransition moveLeft = new TranslateTransition(Duration.seconds(2.5), truckBox);
        moveLeft.setToX(0);

        // Sekwencja animacji: najpierw w prawo, potem w lewo
        SequentialTransition sequence = new SequentialTransition(moveRight, moveLeft);
        sequence.play();
    }

    // Usunięte nieużywane metody moveReversedTruck i moveReversedTruckBox,
    // ponieważ ta funkcjonalność jest teraz zintegrowana z metodami moveTruck i moveTruckBox

    private Image loadImage(String path) {
        try {
            return new Image(Objects.requireNonNull(getClass().getResource(path)).toExternalForm());
        } catch (Exception e) {
            System.err.println("Błąd ładowania obrazu: " + path);
            return null;
        }
    }

    private ImageView createImageView(Image image, double width, boolean preserveRatio) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(width);
        imageView.setPreserveRatio(preserveRatio);
        return imageView;
    }

    private void setBackgroundImage() {
        try {
            Image backgroundImage = loadImage("/background.jpg");
            BackgroundImage background = new BackgroundImage(
                    backgroundImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
            );
            root.setBackground(new javafx.scene.layout.Background(background));
        } catch (Exception e) {
            System.err.println("Nie można załadować tła: " + e.getMessage());
            root.setStyle("-fx-background-color: #f0f0f0;");
        }
    }

    public AnchorPane getView() {
        return root;
    }
}