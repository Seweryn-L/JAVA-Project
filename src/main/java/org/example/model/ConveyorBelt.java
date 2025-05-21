package org.example.model;

import java.util.*;
import java.util.concurrent.Semaphore;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.BooleanProperty;

public class ConveyorBelt {

    //kolejka ktora przechowuje masy cegiel na tasmie
    private final Queue<Integer> bricks = new LinkedList<>();

    //semafory ograniczajace mase i liczbe cegiel na tasmie
    private Semaphore countSemaphore;
    private Semaphore weightSemaphore;

    //Maksymalne parametry tasmy
    private int countMax;
    private int weightMax;
    private int neweightMax;
    //pomocnicza
    private int weightMax1;

    //biezace wartosci
    private int currentWeight = 0;
    private int currentCount = 0;

    //lock do synchronizacji z ciężarówką
    public final Object lock = new Object();

    //flaga widoczna miedzy watkiami
    private volatile boolean isFull = false;

    //suma masy cegiel wyslanych na ciezarowke
    private int sendWeight = 0;
    private final IntegerProperty weightMaxProperty = new SimpleIntegerProperty(weightMax);
    private final IntegerProperty countMaxProperty = new SimpleIntegerProperty(countMax);

    //gettery i setter
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

    private final IntegerProperty brickCountProperty = new SimpleIntegerProperty(0);
    private final IntegerProperty brickWeightProperty = new SimpleIntegerProperty(0);

    public IntegerProperty brickCountProperty() { return brickCountProperty; }
    public IntegerProperty brickWeightProperty() { return brickWeightProperty; }
    public boolean getisFull() { return isFull; }


    //tasma ma dwa parametry
    public ConveyorBelt(int K, int M) {
        this.countMax = K;
        this.weightMax = M;
        this.weightMax1 = M; // zapamiętaj do resetu
        this.countSemaphore = new Semaphore(K, true); //semafor liczby cegieł (fair)
        this.weightSemaphore = new Semaphore(M, true); //semafor masy (fair)
    }

    public void addBrick(int mass) throws InterruptedException {
        while (true) {
            synchronized (this) {
                //sprawdzam czy jest miejsce dla cegly
                if (countSemaphore.availablePermits() > 0 && weightSemaphore.availablePermits() >= mass && currentWeight + mass <= weightMax) {
                    countSemaphore.acquire();
                    weightSemaphore.acquire(mass);

                    // teraz nikt nie moze wejsc w ta sekcje
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
                    //jesli juz nie ma wiecej miejsca informuje o tym
                    if (countSemaphore.availablePermits() == 0 || weightSemaphore.availablePermits() == 0) {
                        synchronized (lock) {
                            isFull = true;
                            lock.notifyAll();
                        }
                    }
                    break; //cegła została dodana, wyjdź z pętli
                }
            }
            Thread.sleep(50); //jesli sie nie udalo
        }
    }

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
            //symulacja oczekiwania ciężarówki przed rozładunkiem
            isFull = false;//reset flagi po rozpoczęciu rozładunku

            //jeśli taśma jest wystarczająco mała
                if (currentCount == countMax) { //zapełniła się przez liczbę cegieł
                    int weightToRelease = currentWeight;
                    if (weightToRelease > 0 ) {
                        weightSemaphore.release(weightToRelease);
                    }
                    int countToRelease = countMax;
                    if (countToRelease > 0) {
                        countSemaphore.release(countToRelease);
                    }
                } else if (currentWeight == weightMax) { //zapełniła się przez masę
                    int weightToRelease = weightMax;
                    if (weightToRelease > 0) {
                        weightSemaphore.release(weightToRelease);
                    }
                    int countToRelease = currentCount;
                    if (countToRelease > 0) {
                        countSemaphore.release(countToRelease);
                    }
                }
                //tutaj modyfikuje maksymalne parametry tasmy ze wzgledu na ciezarowke
            if (Truck.getSize() - sendWeight < weightMax) {
                neweightMax = Truck.getSize() - sendWeight;
                if(weightSemaphore.availablePermits() != 0) {
                    weightSemaphore.acquire(weightSemaphore.availablePermits()-neweightMax);
                }
                weightMax = neweightMax;

            }
            currentCount = 0;
            lock.notifyAll(); //informuje ciezarowke
        }
    }

    //zwracam kopie listy ceigeil
    public synchronized List<Integer> getBricksSnapshot() {
        return new ArrayList<>(bricks);
    }

    public synchronized List<Integer> takeBricksForTruck(int maxLoad) {
        List<Integer> toLoad = new ArrayList<>();
        int total = 0;
        Iterator<Integer> iterator = bricks.iterator();

        //Zdejmuj cegły z taśmy, aż osiągniesz maxLoad
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
        //aktualizuj bieżącą masę taśmy
        currentWeight -= total;
        //aktualizuj UI na wątku JavaFX
        Platform.runLater(() -> {
            brickCountProperty.set(currentCount);
            brickWeightProperty.set(currentWeight);
            countMaxProperty.set(countMax);
            weightMaxProperty.set(weightMax);
        });

        return toLoad;
    }

    public void reset() {
        synchronized (this) {
            //zwalniam tyle pozwolen ile potrzebuje
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
