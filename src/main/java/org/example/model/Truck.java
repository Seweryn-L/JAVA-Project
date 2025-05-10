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

/**
 * Klasa Truck reprezentuje ciężarówkę, która odbiera cegły z taśmy produkcyjnej (ConveyorBelt).
 * Dziedziczy po Thread, więc działa jako osobny wątek.
 */
public class Truck extends Thread {
    // Referencja do taśmy produkcyjnej, z której ciężarówka odbiera cegły
    private ConveyorBelt belt;
    // Maksymalna ładowność ciężarówki
    private static int size;
    // Lista cegieł załadowanych na ciężarówkę (każda cegła reprezentowana przez jej masę)
    private final List<Integer> loadedBricks = new ArrayList<>();
    // Aktualna masa cegieł na ciężarówce
    private static int actual = 0;

    // Właściwości JavaFX do ciągłego monitorowania stanu ciężarówki
    private final IntegerProperty currentWeight = new SimpleIntegerProperty(0);
    private final IntegerProperty maxCapacity = new SimpleIntegerProperty(0);
    private final BooleanProperty informationProperty = new SimpleBooleanProperty(false);

    // Flaga kontrolująca działanie wątku
    private volatile boolean running = true;
    // Timer do obsługi animacji
    private Timer animationTimer;
    // Timer do okresowego aktualizowania właściwości
    private Timer updateTimer;

    // Statyczny getter aktualnej masy cegieł na ciężarówce
    public static int getActual() {
        return actual;
    }

    // Statyczny getter ładowności ciężarówki
    public static int getSize() {
        return size;
    }
    public static void setSize(int Size) {
        size=Size;
    }

    /**
     * Konstruktor ciężarówki.
     * @param size Maksymalna ładowność ciężarówki.
     * @param belt Referencja do taśmy produkcyjnej.
     */
    public Truck(int size, ConveyorBelt belt) {
        super("Truck"); // Nazwij wątek
        Truck.size = size; // Ustawia statyczną ładowność
        this.belt = belt;
        setDaemon(true); // Ustaw jako wątek-demon, aby nie blokował zamykania JVM

        // Inicjalizacja wartości początkowych właściwości
        maxCapacity.set(size);

        // Uruchomienie timera do okresowej aktualizacji właściwości
        startUpdateTimer();
    }

    /**
     * Uruchamia timer do okresowej aktualizacji właściwości
     */
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

    /**
     * Aktualizuje właściwości JavaFX z aktualnymi danymi ciężarówki
     */
    private void updateProperties() {
        if (!running) return;

        Platform.runLater(() -> {
            if (running) {
                currentWeight.set(actual);
                maxCapacity.set(size);
            }
        });
    }

    /**
     * Bezpiecznie zatrzymuje wątek ciężarówki
     */
    public void stopTruck() {
        running = false;
        interrupt();
        cleanupResources();
    }

    /**
     * Czyści zasoby używane przez ciężarówkę
     */
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

    /**
     * Sprawdza, czy można załadować cegłę o podanej masie bez przekroczenia ładowności.
     * @param brickMass Masa cegły.
     * @return true jeśli cegła się zmieści, false w przeciwnym razie.
     */
    public boolean canLoad(int brickMass) {
        return actual + brickMass <= size;
    }

    /**
     * Sprawdza, czy ciężarówka jest pełna.
     * @return true jeśli aktualna masa równa się ładowności.
     */
    public boolean isFull() {
        return actual == size;
    }

    /**
     * Zwraca ilość wolnego miejsca na ciężarówce.
     * @return Wolne miejsce (ładowność minus aktualna masa).
     */
    public static int Space() {
        return size - actual;
    }

    /**
     * Rozładowuje ciężarówkę: czyści listę cegieł, resetuje licznik masy i informuje taśmę.
     * Synchronizowana, aby zapewnić bezpieczeństwo wątkowe.
     */
    public synchronized void unload() {
        System.out.println("ROZLADOWANO");
        loadedBricks.clear();   // Usuwa wszystkie cegły z ciężarówki
        actual = 0;            // Resetuje aktualną masę

        // Aktualizuj UI tylko jeśli aplikacja nadal działa
        if (running) {
            updateProperties(); // Aktualizuj wszystkie właściwości
            belt.reset();          // Resetuje stan taśmy (np. przygotowuje na nowe cegły)

            // Powiadamia wszystkie wątki czekające na locku taśmy, że ciężarówka jest rozładowana
            synchronized (belt.lock) {
                belt.lock.notifyAll();
            }
        }
    }

    // Zwraca kopię listy załadowanych cegieł (aby nie można było jej modyfikować z zewnątrz)
    public List<Integer> getLoadedBricks() {
        return new ArrayList<>(loadedBricks);
    }

    // Właściwości do monitorowania ciężarówki
    public IntegerProperty currentWeightProperty() {
        return currentWeight;
    }

    public IntegerProperty maxCapacityProperty() {
        return maxCapacity;
    }

    public BooleanProperty informationProperty() {
        return informationProperty;
    }

    /**
     * Ładuje cegłę na ciężarówkę.
     * Synchronizowana, aby zapewnić bezpieczeństwo wątkowe.
     * @param brick Masa cegły.
     */
    public synchronized void load(int brick) {
        // Sprawdza, czy załadowanie cegły nie przekroczy ładowności
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

    /**
     * Główna metoda wątku ciężarówki.
     * W pętli:
     * 1. Czeka, aż taśma będzie pełna (metoda blokująca).
     * 2. Pobiera cegły z taśmy (do ładowności ciężarówki).
     * 3. Ładuje cegły na ciężarówkę.
     * 4. Jeśli ciężarówka jest pełna, rozładowuje ją.
     * 5. Obsługuje przerwanie wątku.
     */
    @Override
    public void run() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    // Czeka, aż taśma będzie pełna (metoda powinna blokować do tego momentu)
                    belt.waitForFullBelt();

                    // Sprawdź, czy wątek nadal powinien działać po odblokowaniu
                    if (!running || Thread.currentThread().isInterrupted()) {
                        break;
                    }

                    // Pobiera cegły z taśmy do załadowania na ciężarówkę (do ładowności)
                    List<Integer> bricksToLoad = belt.takeBricksForTruck(size);

                    // Ładuje każdą cegłę na ciężarówkę
                    for (Integer brick : bricksToLoad) {
                        if (!running || Thread.currentThread().isInterrupted()) {
                            break;
                        }
                        load(brick);

                        // Krótka pauza dla efektu ładowania
                        Thread.sleep(50);
                    }

                    // Wyświetla aktualny stan ciężarówki
                    System.out.println("Aktualny stan ciezarowki: " + actual + "/" + size +
                            " (liczba cegieł: " + loadedBricks.size() + ")");

                    // Jeśli ciężarówka jest pełna, rozładowuje ją
                    if (isFull() && running && !Thread.currentThread().isInterrupted()) {
                        // Trigger animacji tylko jeśli aplikacja nadal działa
                        if (running) {
                            Platform.runLater(() -> {
                                if (running) {
                                    informationProperty.set(true);  // Trigger animacji

                                    // Anuluj stary timer jeśli istnieje
                                    if (animationTimer != null) {
                                        animationTimer.cancel();
                                    }

                                    // Stwórz nowy timer
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
                                            1000  // Wyłącz informację po 1 sekundzie
                                    );
                                }
                            });
                        }

                        // Poczekaj tylko jeśli nadal działamy
                        if (running && !Thread.currentThread().isInterrupted()) {
                            Thread.sleep(3000);
                        }

                        // Rozładuj tylko jeśli nadal działamy
                        if (running && !Thread.currentThread().isInterrupted()) {
                            unload();
                        }
                    }
                } catch (InterruptedException e) {
                    // Wątek został przerwany podczas czekania
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
            // Kod wykonywany zawsze przy zakończeniu wątku
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