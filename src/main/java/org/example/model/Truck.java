package org.example.model;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.BooleanProperty;
import java.util.Timer;
import java.util.TimerTask;


public class Truck extends Thread {
    //referencja do tasmy
    private ConveyorBelt belt;
    private static int size;
    //lista cegiel zaladowanych
    private final List<Integer> loadedBricks = new ArrayList<>();
    private static int actual = 0;

    // łaściwości JavaFX do ciągłego monitorowania stanu ciężarówki
    private final IntegerProperty currentWeight = new SimpleIntegerProperty(0);
    private final IntegerProperty maxCapacity = new SimpleIntegerProperty(0);
    private final BooleanProperty informationProperty = new SimpleBooleanProperty(false);

    private volatile boolean running = true;
    private Timer animationTimer;
    private Timer updateTimer;


    public static int getActual() {
        return actual;
    }
    public static int getSize() {
        return size;
    }
    public static void setSize(int Size) {
        size=Size;
    }

    //parametrem jest wielkosc i referencja do tasmy z ktorej korzysta ciężarówka
    public Truck(int size, ConveyorBelt belt) {
        super("Truck");
        Truck.size = size;
        this.belt = belt;
        setDaemon(true); //aby nie blokowal zamykania
        maxCapacity.set(size);
        startUpdateTimer();
    }

    private void startUpdateTimer() {
        updateTimer = new Timer(true); // true = daemon timer
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (running) {
                    updateProperties();
                }
            }
        }, 0, 100); // Aktualizacja co 100ms
    }
    private void updateProperties() {
        if (!running) return;

        Platform.runLater(() -> {
            if (running) {
                currentWeight.set(actual);
                maxCapacity.set(size);
            }
        });
    }
    public void stopTruck() {
        running = false;
        interrupt();
        cleanupResources();
    }

    private void cleanupResources() {
        // Anuluj wszystkie zaplanowane zadania timerów
        if (animationTimer != null) {
            animationTimer.cancel();
            animationTimer = null;
        }

        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }

        System.out.println("Truck - zasoby wyczyszczone");
    }

    public boolean isFull() {
        return actual == size;
    }

    public synchronized void unload() {
        System.out.println("ROZLADOWANO");
        loadedBricks.clear();   //usuwa wszystkie cegły z ciężarówki
        actual = 0;            //resetuje aktualną masę

        // aktualizacja jesli aplikacja dziala
        if (running) {
            updateProperties(); //aktualizacja właściwości
            belt.reset();          //reset tasmy po rozladowaniu

            //powiadamia watki ze ciezarowka rozladowana i mozna dalej pracowac
            synchronized (belt.lock) {
                belt.lock.notifyAll();
            }
        }
    }
    //monitorowanie
    public IntegerProperty currentWeightProperty() {
        return currentWeight;
    }
    public IntegerProperty maxCapacityProperty() {
        return maxCapacity;
    }
    public BooleanProperty informationProperty() {
        return informationProperty;
    }

    public synchronized void load(int brick) {
        //dla bezpieczenstwa sprawdzam czy moge zaladowac
        if (actual + brick > size) {
            throw new IllegalStateException("Próba przeladowania cięzarówki");
        }
        loadedBricks.add(brick);           // Dodaje cegłę do listy
        actual += brick;                   // Zwiększa aktualną masę

        // Aktualizacja właściwości po załadowaniu cegły
        if (running) {
            updateProperties();
        }
    }

    @Override
    public void run() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    //czeka az tasma bedzie pelna
                    belt.waitForFullBelt();
                    if (!running || Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    // pobiera cegly z tasmy
                    List<Integer> bricksToLoad = belt.takeBricksForTruck(size);

                    // Ładuje każdą cegłę na ciężarówkę
                    for (Integer brick : bricksToLoad) {
                        if (!running || Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        load(brick);
                        //pauza zeby byl widoczny efekt ladowania
                        Thread.sleep(50);
                    }

                    //stan ciezarowki po ladowaniu
                    System.out.println("Aktualny stan ciezarowki: " + actual + "/" + size +
                            " (liczba cegieł: " + loadedBricks.size() + ")");

                    //gdy ciezarowka pelna to ja rozladuj
                    if (isFull() && running && !Thread.currentThread().isInterrupted()) {
                        //trigger animacji
                        if (running) {
                            Platform.runLater(() -> {
                                if (running) {
                                    informationProperty.set(true);
                                    if (animationTimer != null) {
                                        animationTimer.cancel();
                                    }
                                    animationTimer = new Timer();
                                    animationTimer.schedule(
                                            new TimerTask() {
                                                @Override
                                                public void run() {
                                                    if (running) {
                                                        Platform.runLater(() -> {
                                                            if (running) {
                                                                informationProperty.set(false);
                                                            }
                                                        });
                                                    }
                                                }
                                            },
                                            1000
                                    );
                                }
                            });
                        }

                        if (running && !Thread.currentThread().isInterrupted()) {
                            Thread.sleep(3000);
                        }

                        if (running && !Thread.currentThread().isInterrupted()) {
                            unload();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                    break;
                } catch (Exception e) {
                    if (running) {
                        System.err.println("Truck błąd: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            //zawsze gdy watek sie zakonczy
            cleanupResources();
            System.out.println("Truck watek zakonczony");
        }
    }

    @Override
    public void interrupt() {
        running = false;
        super.interrupt();
    }
}