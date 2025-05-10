package org.example.model;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.example.app.AppMain;

/**
 * Klasa Worker reprezentuje pracownika (wątek), który dodaje cegły na taśmę produkcyjną.
 * Dziedziczy po klasie Thread, przez co każdy Worker działa jako osobny wątek.
 */
public class Worker extends Thread {
    // Referencja do taśmy produkcyjnej, pobierana statycznie z klasy głównej aplikacji.
    private final ConveyorBelt belt = AppMain.belt;
    // Masa cegły, którą worker dodaje na taśmę.
    private final int brickMass;
    // Właściwość logiczna, która może być obserwowana przez UI (np. do sygnalizowania stanu).
    private final BooleanProperty information = new SimpleBooleanProperty(false);
    // Czas (w milisekundach), co ile worker dodaje cegłę na taśmę.
    private final int time;
    // Dodajemy flagę informującą, czy udało się dodać cegłę
    private volatile boolean brickAdded = false;
    // Flaga kontrolująca działanie wątku
    private volatile boolean running = true;

    /**
     * Konstruktor klasy Worker.
     * @param name Nazwa wątku (pracownika).
     * @param brickMass Masa cegły dodawanej przez tego worker'a.
     * @param time Opóźnienie (w ms) pomiędzy kolejnymi dodaniami cegły.
     */
    public Worker(String name, int brickMass, int time) {
        super(name); // Ustawia nazwę wątku.
        this.brickMass = brickMass;
        this.time = time;
        // Ustawiamy wątek jako demon, aby nie blokował zamykania aplikacji
        setDaemon(true);
    }

    /**
     * Zwraca właściwość logiczną, która może być bindowana do UI.
     * Dzięki temu UI może reagować na zmiany stanu tego worker'a.
     */
    public BooleanProperty informationProperty() {
        return information;
    }

    /**
     * Bezpiecznie zatrzymuje wątek
     */
    public void stopWorker() {
        running = false;
        interrupt();
    }

    /**
     * Główna metoda wątku. Worker w nieskończonej pętli:
     * 1. Próbuje dodać cegłę na taśmę.
     * 2. Jeśli się udało, wysyła notyfikację do UI.
     * 3. Usypia wątek na podany czas.
     * Pętla działa do momentu przerwania wątku (InterruptedException).
     */
    @Override
    public void run() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                // Resetujemy flagę dodania cegły
                brickAdded = false;

                try {
                    // Dodanie cegły na taśmę
                    belt.addBrick(brickMass);

                    // Ustawiamy flagę sukcesu
                    brickAdded = true;

                    // Sygnalizujemy UI o dodaniu cegły - wysyłamy true
                    if (!Thread.currentThread().isInterrupted() && running) {
                        Platform.runLater(() -> {
                            // Sprawdzamy, czy udało się dodać cegłę
                            if (brickAdded) {
                                // Ustawiamy flagę informacyjną na true
                                informationProperty().set(true);

                                // Reset flagi po krótkim czasie, aby powiadomienie było widoczne
                                Thread resetThread = new Thread(() -> {
                                    try {
                                        Thread.sleep(500);
                                        if (Platform.isFxApplicationThread()) {
                                            informationProperty().set(false);
                                        } else {
                                            Platform.runLater(() -> informationProperty().set(false));
                                        }
                                    } catch (InterruptedException ex) {
                                        Thread.currentThread().interrupt();
                                    }
                                });
                                resetThread.setDaemon(true);
                                resetThread.start();
                            }
                        });
                    }

                } catch (Exception e) {
                    if (running) {  // Tylko loguj błąd, jeśli wątek nie jest celowo zatrzymywany
                        System.err.println(getName() + " problem z dodaniem cegly: " + e.getMessage());
                    }
                    Thread.currentThread().interrupt();
                    running = false;
                }

                // Uśpienie wątku na zadany czas jeśli nadal działamy
                if (running && !Thread.currentThread().isInterrupted()) {
                    Thread.sleep(time);
                } else {
                    break; // Przerwij pętlę jeśli wątek ma być zatrzymany
                }
            }
        } catch (InterruptedException e) {
            // Normalne przerwanie wątku - nie wymaga dodatkowej obsługi
        } catch (Exception e) {
            System.err.println(getName() + " nieoczekiwany błąd: " + e.getMessage());
        } finally {
            System.out.println(getName() + " zostal zakonczony");
            running = false;
        }
    }

    @Override
    public void interrupt() {
        running = false;
        super.interrupt();
    }
}