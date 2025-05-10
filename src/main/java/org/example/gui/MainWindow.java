package org.example.gui;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.app.AppMain;
import javafx.scene.control.Label;
import org.example.config.AppConfig;
import org.example.config.ConfigLoader;
import org.example.model.ConveyorBelt;
import javafx.scene.layout.VBox;
import org.example.model.Truck;
import org.example.model.Worker;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MainWindow extends Application {
    private final Background background = new Background();
    private final StackPane root = new StackPane();
    private WorkersGui workersGui;
    private Stage primaryStageRef;
    private ConveyorBelt belt;
    private final CountDownLatch uiInitLatch = new CountDownLatch(1);
    private VBox truckBox; // Dodajemy referencję do truckBox jako pole klasy

    /**
     * Inicjalizuje podstawowe komponenty UI
     */
    private void initializeUI(Stage primaryStage) {
        try {
            // Add background
            root.getChildren().add(background.getView());

            // Create info box
            VBox infoBox = createInfoBox();
            this.truckBox = createTruckBox(); // Przypisujemy do pola klasy

            AnchorPane backgroundPane = background.getView();
            backgroundPane.getChildren().add(truckBox);
            AnchorPane.setBottomAnchor(truckBox, 70.0);
            AnchorPane.setRightAnchor(truckBox, 150.0);

            // Create scene with CSS
            Scene scene = new Scene(root, 900, 400);

            // Add CSS styling
            try {
                String cssPath = Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm();
                scene.getStylesheets().add(cssPath);
            } catch (Exception e) {
                System.err.println("Failed to load CSS: " + e.getMessage());
            }

            // Set up and show stage
            primaryStage.setScene(scene);
            primaryStage.setTitle("Brick Transport - JavaFX");
            primaryStage.setResizable(false);

            // Add info boxes to root
            root.getChildren().add(infoBox);
            StackPane.setAlignment(infoBox, Pos.TOP_CENTER);




            // Dodaj obsługę zamknięcia okna
            primaryStage.setOnCloseRequest(event -> {
                event.consume(); // Zapobiega automatycznemu zamknięciu
                stop(); // Wywołaj metodę stop, która zamknie aplikację
            });

            // Show stage
            primaryStage.show();

            // Sygnalizujemy, że UI jest gotowe
            Platform.runLater(() -> uiInitLatch.countDown());

        } catch (Exception e) {
            System.err.println("Error initializing UI: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Inicjalizuje WorkersGui
     */
    private void initializeWorkersGui() {
        try {

            // Inicjalizacja WorkersGui
            workersGui = new WorkersGui(primaryStageRef);

        } catch (Exception e) {
            System.err.println("Error initializing WorkersGui: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize WorkersGui", e);
        }
    }

    /**
     * Inicjalizuje aktorów (pracowników i ciężarówkę)
     */
    private void initializeActors() {
        try {
            AppConfig config = ConfigLoader.loadConfig();
            // Utwórz pracowników
            AppMain.initializeActors(config);

            // Ustaw listenery PRZED uruchomieniem wątków
            setupWorkerListeners();

        } catch (Exception e) {
            System.err.println("Error initializing actors: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize actors", e);
        }
    }

    /**
     * Uruchamia wątki pracowników i ciężarówki
     */
    private void startThreads() {
        // Uruchamiamy z opóźnieniem, aby zapewnić że UI jest w pełni gotowe
        Platform.runLater(() -> {
            try {
                Thread.sleep(1000); // Krótsze opóźnienie, 1s powinno wystarczyć

                // Uruchom wątki
                if (AppMain.worker1 != null) AppMain.worker1.start();
                if (AppMain.worker2 != null) AppMain.worker2.start();
                if (AppMain.worker3 != null) AppMain.worker3.start();
                if (AppMain.truck != null) AppMain.truck.start();

                System.out.println("All threads started successfully");

            } catch (Exception e) {
                System.err.println("Error starting threads: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Creates the info box with conveyor belt status
     */
    private VBox createInfoBox() {
        // Create UI labels
        Label constantLabel = new Label("Stan Taśmy");
        constantLabel.getStyleClass().add("title-label");

        Label countLabel = new Label();
        countLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> String.format("Liczba \uD83E\uDDF1 %d / %d",
                                belt.brickCountProperty().get(),
                                belt.countMaxProperty().get()),
                        belt.brickCountProperty(),
                        belt.countMaxProperty()
                )
        );

        Label weightLabel = new Label();
        weightLabel.textProperty().bind(
                Bindings.createStringBinding(
                        () -> String.format("Masa: \uD83E\uDDF1 %d / %d",
                                belt.brickWeightProperty().get(),
                                belt.weightMaxProperty().get()),
                        belt.brickWeightProperty(),
                        belt.weightMaxProperty()
                )
        );

        // Create info box
        VBox infoBox = new VBox(5);
        infoBox.getStyleClass().add("info-box");
        infoBox.getChildren().addAll(
                constantLabel,
                countLabel,
                weightLabel
        );

        infoBox.setMaxWidth(200);
        infoBox.setMaxHeight(50);

        return infoBox;
    }

    /**
     * Creates the truck info box
     */
    public VBox createTruckBox() {
        Label constantLabel = new Label("Stan Ciężarówki");
        constantLabel.getStyleClass().add("title-label");

        Label weightLabel = new Label();
        // Bind zostanie ustawiony, gdy Truck jest zainicjalizowany
        Platform.runLater(() -> {
            if (AppMain.truck != null) {
                weightLabel.textProperty().bind(
                        Bindings.createStringBinding(
                                () -> String.format("Liczba \uD83E\uDDF1 %d / %d",
                                        AppMain.truck.currentWeightProperty().get(),
                                        AppMain.truck.maxCapacityProperty().get()),
                                AppMain.truck.currentWeightProperty(),
                                AppMain.truck.maxCapacityProperty()
                        )
                );
            }
        });

        // Create info box
        VBox truckBox = new VBox(5);
        truckBox.getStyleClass().add("truck-label-box");
        truckBox.getChildren().addAll(
                constantLabel,
                weightLabel
        );

        truckBox.setMaxWidth(200);
        truckBox.setMaxHeight(50);

        return truckBox;
    }

    /**
     * Set up listeners for worker notification events
     */
    private void setupWorkerListeners() {
        // Ensure we have a valid WorkersGui
        if (workersGui == null) {
            System.err.println("WorkersGui not initialized!");
            workersGui = new WorkersGui(primaryStageRef);
        }

        // Ensure stage reference is up to date
        workersGui.updateStage(primaryStageRef);

        // Set up listeners for worker notifications
        if (AppMain.worker1 != null) {
            AppMain.worker1.informationProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    Platform.runLater(() -> workersGui.showNotification(true, "P1"));
                }
            });
        }

        if (AppMain.worker2 != null) {
            AppMain.worker2.informationProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    Platform.runLater(() -> workersGui.showNotification(true, "P2"));
                }
            });
        }

        if (AppMain.worker3 != null) {
            AppMain.worker3.informationProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    Platform.runLater(() -> workersGui.showNotification(true, "P3"));
                }
            });
        }

        // Set up listener for belt animation
        if (belt != null) {
            belt.informationProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    Platform.runLater(() -> background.moveBrick());
                }
            });
        }

        // Set up listener for truck animation
        if (AppMain.truck != null) {
            AppMain.truck.informationProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    Platform.runLater(() -> {
                        // Wywołujemy animację ciężarówki
                        background.moveTruck();
                        // Wywołujemy animację przemieszczenia truckBox
                        background.moveTruckBox(truckBox);
                    });
                }
            });
        }
    }

    public Stage getPrimaryStageRef() {
        return primaryStageRef;
    }

    public void setPrimaryStageRef(Stage primaryStageRef) {
        this.primaryStageRef = primaryStageRef;
        if (workersGui != null) {
            workersGui.updateStage(primaryStageRef);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Store reference to primary stage
            this.primaryStageRef = primaryStage;
            // Uzyskanie referencji do taśmy produkcyjnej
            this.belt = AppMain.belt;

            System.out.println("Starting MainWindow setup");

            // =========== ETAP 1: Inicjalizacja podstawowego UI ===========
            initializeUI(primaryStage);

            // =========== ETAP 2: Inicjalizacja WorkersGui ===========
            initializeWorkersGui();

            // =========== ETAP 3: Inicjalizacja workerów i ciężarówki ===========
            initializeActors();

            // =========== ETAP 4: Uruchomienie wątków ===========
            startThreads();

        } catch (Exception e) {
            System.err.println("Error initializing MainWindow: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }
    }
    @Override
    public void stop() {
        // Cleanup when the application is closing
        try {
            System.out.println("Zatrzymywanie aplikacji...");

            // Zatrzymujemy wątki pracowników
            if (AppMain.worker1 != null) {
                AppMain.worker1.stopWorker();
                System.out.println("Worker1 zatrzymany");
            }

            if (AppMain.worker2 != null) {
                AppMain.worker2.stopWorker();
                System.out.println("Worker2 zatrzymany");
            }

            if (AppMain.worker3 != null) {
                AppMain.worker3.stopWorker();
                System.out.println("Worker3 zatrzymany");
            }

            // Zatrzymujemy ciężarówkę
            if (AppMain.truck != null) {
                AppMain.truck.stopTruck(); // Używamy dedykowanej metody stopTruck
                System.out.println("Truck zatrzymany");
            }

            // Daj wątkom chwilę na zakończenie
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("Wszystkie watki zatrzymane");

        } catch (Exception e) {
            System.err.println("Error stopping threads: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Upewnij się, że aplikacja na pewno się zamknie
            Platform.exit();
            System.exit(0);
        }
    }
}