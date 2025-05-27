package com.example.healthmonitor;

public class HealthRecord {
    private String time;
    private int spo2;
    private int heartRate;
    private double temperature;

    public HealthRecord(String time, int spo2, int heartRate, double temperature) {
        this.time = time;
        this.spo2 = spo2;
        this.heartRate = heartRate;
        this.temperature = temperature;
    }

    public String getTime() {
        return time;
    }

    public int getSpO2() {
        return spo2;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public double getTemperature() {
        return temperature;
    }
}
