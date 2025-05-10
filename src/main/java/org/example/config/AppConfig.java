package org.example.config;

public class AppConfig {
    // Parametry taśmy
    private int beltCountMax;
    private int beltWeightMax;

    // Parametry ciężarówki
    private int truckCapacity;

    // Parametry pracowników
    private WorkerConfig worker1;
    private WorkerConfig worker2;
    private WorkerConfig worker3;

    // Gettery i settery
    public int getBeltCountMax() { return beltCountMax; }
    public void setBeltCountMax(int beltCountMax) { this.beltCountMax = beltCountMax; }

    public int getBeltWeightMax() { return beltWeightMax; }
    public void setBeltWeightMax(int beltWeightMax) { this.beltWeightMax = beltWeightMax; }

    public int getTruckCapacity() { return truckCapacity; }
    public void setTruckCapacity(int truckCapacity) { this.truckCapacity = truckCapacity; }

    public WorkerConfig getWorker1() { return worker1; }
    public void setWorker1(WorkerConfig worker1) { this.worker1 = worker1; }

    public WorkerConfig getWorker2() { return worker2; }
    public void setWorker2(WorkerConfig worker2) { this.worker2 = worker2; }

    public WorkerConfig getWorker3() { return worker3; }
    public void setWorker3(WorkerConfig worker3) { this.worker3 = worker3; }

    // Klasa wewnętrzna dla konfiguracji pracownika
    public static class WorkerConfig {
        private int brickMass;
        private int interval;

        public int getBrickMass() { return brickMass; }
        public void setBrickMass(int brickMass) { this.brickMass = brickMass; }

        public int getInterval() { return interval; }
        public void setInterval(int interval) { this.interval = interval; }
    }
}
