package com.smartcampus.model;

/**
 * Represents a single reading event captured by a sensor.
 * Each reading records the measured value and the timestamp
 * when the measurement was taken.
 */
public class SensorReading {

    private String id;          // Unique reading event ID (UUID recommended)
    private long timestamp;     // Epoch time (ms) when the reading was captured
    private double value;       // The actual metric value recorded by the hardware

    // Default constructor (required for JSON deserialization)
    public SensorReading() {
    }

    // Parameterized constructor
    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
