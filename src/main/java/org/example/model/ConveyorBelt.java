package org.example.model;

import java.util.*;
import java.util.concurrent.Semaphore;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.BooleanProperty;

/**
 * Klasa ConveyorBelt reprezentuje taśmę produkcyjną, na którą pracownicy dokładają cegły,
 * a ciężarówka je odbiera. Kontroluje zarówno liczbę cegieł, jak i ich łączną masę.
 * Synchronizacja odbywa się za pomocą semaforów oraz własnego locka.
 */
public class ConveyorBelt {

    // Kolejka przechowująca masy cegieł na taśmie
    private final Queue<Integer> bricks = new LinkedList<>();

    // Semafor ograniczający liczbę cegieł na taśmie
    private Semaphore countSemaphore;
    // Semafor ograniczający łączną masę cegieł na taśmie
    private Semaphore weightSemaphore;

    // Maksymalna liczba cegieł na taśmie
    private int countMax;
    // Maksymalna masa cegieł na taśmie (może się zmieniać dynamicznie)
    private int weightMax;
    private int neweightMax;
    // Początkowa maksymalna masa (do resetowania)
    private int weightMax1;

    // Bieżąca masa cegieł na taśmie
    private int currentWeight = 0;
    // Bieżąca liczba cegieł na taśmie
    private int currentCount = 0;

    // Obiekt do synchronizacji komunikacji między wątkami (np. z ciężarówką)
    public final Object lock = new Object();

    // Flaga informująca, czy taśma jest pełna (volatile: widoczna między wątkami)
    private volatile boolean isFull = false;

    // Suma masy cegieł przekazanych do ciężarówki (do obliczeń przy dynamicznej zmianie pojemności)
    private int sendWeight = 0;
    private final IntegerProperty weightMaxProperty = new SimpleIntegerProperty(weightMax);
    private final IntegerProperty countMaxProperty = new SimpleIntegerProperty(countMax);

    public IntegerProperty weightMaxProperty() { return weightMaxProperty; }
    public int getWeightMax() { return weightMaxProperty.get(); }
    public void setWeightMax(int value) { weightMaxProperty.set(value); }

    private final BooleanProperty informationProperty = new SimpleBooleanProperty(false);

    public BooleanProperty informationProperty() {
        return informationProperty;
    }
    public void setInformation(boolean value) { informationProperty.set(value); }

    public IntegerProperty countMaxProperty() {return countMaxProperty;}
    public int getCountMax() { return countMaxProperty.get(); }
    public void setCountMax(int value) { countMaxProperty.set(value); }
    // Właściwości JavaFX do powiązania z UI (liczba cegieł i masa)
    private final IntegerProperty brickCountProperty = new SimpleIntegerProperty(0);
    private final IntegerProperty brickWeightProperty = new SimpleIntegerProperty(0);

    // Gettery do powiązań z UI
    public IntegerProperty brickCountProperty() { return brickCountProperty; }
    public IntegerProperty brickWeightProperty() { return brickWeightProperty; }
    public boolean getisFull() { return isFull; }

    /**
     * Konstruktor taśmy.
     * @param K Maksymalna liczba cegieł.
     * @param M Maksymalna masa cegieł.
     */
    public ConveyorBelt(int K, int M) {
        this.countMax = K;
        this.weightMax = M;
        this.weightMax1 = M; // zapamiętaj do resetu
        this.countSemaphore = new Semaphore(K, true); // semafor liczby cegieł (fair)
        this.weightSemaphore = new Semaphore(M, true); // semafor masy (fair)
    }

    /**
     * Dodaje cegłę na taśmę produkcyjną.
     * Synchronizuje się na semaforach oraz na this (dla bezpieczeństwa).
     * @param mass Masa dodawanej cegły.
     * @throws InterruptedException jeśli wątek zostanie przerwany.
     */
    public void addBrick(int mass) throws InterruptedException {
        while (true) {
            synchronized (this) {
                // Sprawdź, czy jest miejsce na liczbę cegieł i masę
                if (countSemaphore.availablePermits() > 0 && weightSemaphore.availablePermits() >= mass && currentWeight + mass <= weightMax) {
                    countSemaphore.acquire();
                    weightSemaphore.acquire(mass);

                    // Teraz już nikt nie może wejść do tej sekcji zanim nie skończysz
                    bricks.add(mass);
                    currentWeight += mass;
                    sendWeight += mass;
                    currentCount++;

                    Platform.runLater(() -> {
                        brickCountProperty.set(currentCount);
                        brickWeightProperty.set(currentWeight);
                        countMaxProperty.set(countMax);
                        weightMaxProperty.set(weightMax);
                    });

                    System.out.println("Dodano cegle " + currentWeight + "/" + weightMax + " +" + mass + " (" + currentCount + ")");
                    System.out.println("semafor wagi " + weightSemaphore.availablePermits());
                    System.out.println("semafor liczby " + countSemaphore.availablePermits());

                    if (countSemaphore.availablePermits() == 0 || weightSemaphore.availablePermits() == 0) {
                        synchronized (lock) {
                            isFull = true;
                            lock.notifyAll();
                        }
                    }
                    break; // cegła została dodana, wyjdź z pętli
                }
            }
            Thread.sleep(50); // poczekaj i spróbuj ponownie
        }
    }


    /**
     * Metoda blokująca, która czeka aż taśma się zapełni (przez liczbę lub masę).
     * Po zapełnieniu może dynamicznie zmieniać pojemność taśmy, by dopasować do ciężarówki.
     * @throws InterruptedException jeśli wątek zostanie przerwany.
     */
    public void waitForFullBelt() throws InterruptedException {
        synchronized (lock) {
            while (!isFull) {
                lock.wait(); // czekaj, aż taśma się zapełni
            }
            Platform.runLater(() -> {
                informationProperty.set(true);  // Trigger animacji
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                Platform.runLater(() -> informationProperty.set(false));
                            }
                        },
                        1000  // Wyłącz informację po 4 sekundach
                );
            });
            Thread.sleep(3000);
            // symulacja oczekiwania ciężarówki przed rozładunkiem
            isFull = false;// reset flagi po rozpoczęciu rozładunku

            // jeśli taśma jest wystarczająco mała
                if (currentCount == countMax) { // zapełniła się przez liczbę cegieł
                    int weightToRelease = currentWeight;
                    if (weightToRelease > 0 ) {
                        weightSemaphore.release(weightToRelease);
                    }
                    int countToRelease = countMax;
                    if (countToRelease > 0) {
                        countSemaphore.release(countToRelease);
                    }
                } else if (currentWeight == weightMax) { // zapełniła się przez masę
                    int weightToRelease = weightMax;
                    if (weightToRelease > 0) {
                        weightSemaphore.release(weightToRelease);
                    }
                    int countToRelease = currentCount;
                    if (countToRelease > 0) {
                        countSemaphore.release(countToRelease);
                    }
                }

            if (Truck.getSize() - sendWeight < weightMax) {
                neweightMax = Truck.getSize() - sendWeight;
                if(weightSemaphore.availablePermits() != 0) {
                    weightSemaphore.acquire(weightSemaphore.availablePermits()-neweightMax);
                }
                weightMax = neweightMax;

            }

            currentCount = 0;


            lock.notifyAll();
        }
    }


    /**
     * Zwraca kopię listy cegieł na taśmie (synchronizowana).
     * @return Lista mas cegieł.
     */
    public synchronized List<Integer> getBricksSnapshot() {
        return new ArrayList<>(bricks);
    }

    /**
     * Pobiera cegły z taśmy do załadunku na ciężarówkę.
     * Usuwa je z taśmy i aktualizuje bieżącą masę.
     * @param maxLoad Maksymalna masa do załadowania.
     * @return Lista cegieł do załadowania.
     */
    public synchronized List<Integer> takeBricksForTruck(int maxLoad) {
        List<Integer> toLoad = new ArrayList<>();
        int total = 0;
        Iterator<Integer> iterator = bricks.iterator();

        // Zdejmuj cegły z taśmy, aż osiągniesz maxLoad
        while (iterator.hasNext()) {
            int brick = iterator.next();
            if (total + brick <= maxLoad) {
                toLoad.add(brick);
                total += brick;
                iterator.remove();
            } else {
                break;
            }
        }
        // Aktualizuj bieżącą masę taśmy
        currentWeight -= total;

        // Aktualizuj UI na wątku JavaFX
        Platform.runLater(() -> {
            brickCountProperty.set(currentCount);
            brickWeightProperty.set(currentWeight);
            countMaxProperty.set(countMax);
            weightMaxProperty.set(weightMax);
        });

        return toLoad;
    }

    /**
     * Resetuje stan taśmy do wartości początkowych.
     * Czyści kolejkę, resetuje semafory i zmienne.
     */
    public void reset() {
        synchronized (this) {
            // Zamiast tworzyć nowe semafory, zwolnij tyle pozwoleń, ile trzeba
            int countToRelease = countMax - countSemaphore.availablePermits();
            if (countToRelease > 0) countSemaphore.release(countToRelease);

            int weightToRelease = weightMax1 - weightSemaphore.availablePermits();
            if (weightToRelease > 0) weightSemaphore.release(weightToRelease);

            currentCount = 0;
            currentWeight = 0;
            Platform.runLater(() -> {
                brickCountProperty.set(currentCount);
                brickWeightProperty.set(currentWeight);
                countMaxProperty.set(countMax);
                weightMaxProperty.set(weightMax);
            });

            sendWeight = 0;
            bricks.clear();
            weightMax = weightMax1;
        }
    }


}
