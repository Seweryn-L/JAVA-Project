package org.example.app;

import org.example.config.AppConfig;
import org.example.config.ConfigLoader;
import org.example.gui.MainWindow;
import org.example.model.ConveyorBelt;
import org.example.model.Truck;
import org.example.model.Worker;

public class AppMain {
    public static Worker worker1;
    public static Worker worker2;
    public static Worker worker3;
    public static Truck truck;
    public static final ConveyorBelt belt = new ConveyorBelt(15, 29);
    public static void initializeActors(AppConfig config) {
        worker1 = new Worker("P1", config.getWorker1().getBrickMass(), config.getWorker1().getInterval());
        worker2 = new Worker("P2", config.getWorker2().getBrickMass(), config.getWorker2().getInterval());
        worker3 = new Worker("P3", config.getWorker3().getBrickMass(), config.getWorker3().getInterval());
        truck = new Truck(config.getTruckCapacity(), belt);
    }

    public static void main(String[] args) {
        AppConfig config = ConfigLoader.loadConfig();
        MainWindow.launch(MainWindow.class, args); // Tylko to!

    }
}
