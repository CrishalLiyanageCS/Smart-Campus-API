package com.smartcampus.repository;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory data store for the Smart Campus API.
 * 
 * Uses ConcurrentHashMap for thread-safe access since JAX-RS creates
 * a new resource instance per request by default, and multiple requests
 * may be processed concurrently.
 * 
 * Pre-populated with sample data for testing and demonstration.
 */
public class DataStore {

    // Primary data stores
    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private static final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private static final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    // Initialize with sample data
    static {
        // ===== Sample Rooms =====
        Room room1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room room2 = new Room("ENG-102", "Engineering Lab A", 30);
        Room room3 = new Room("SCI-201", "Science Lecture Hall", 120);

        rooms.put(room1.getId(), room1);
        rooms.put(room2.getId(), room2);
        rooms.put(room3.getId(), room3);

        // ===== Sample Sensors =====
        Sensor sensor1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor sensor2 = new Sensor("CO2-001", "CO2", "ACTIVE", 415.0, "LIB-301");
        Sensor sensor3 = new Sensor("OCC-001", "Occupancy", "ACTIVE", 15.0, "ENG-102");
        Sensor sensor4 = new Sensor("TEMP-002", "Temperature", "MAINTENANCE", 0.0, "ENG-102");

        sensors.put(sensor1.getId(), sensor1);
        sensors.put(sensor2.getId(), sensor2);
        sensors.put(sensor3.getId(), sensor3);
        sensors.put(sensor4.getId(), sensor4);

        // Link sensors to their rooms
        room1.getSensorIds().add(sensor1.getId());
        room1.getSensorIds().add(sensor2.getId());
        room2.getSensorIds().add(sensor3.getId());
        room2.getSensorIds().add(sensor4.getId());
        // room3 has no sensors (useful for testing DELETE)

        // Initialize empty reading lists for each sensor
        sensorReadings.put(sensor1.getId(), new ArrayList<>());
        sensorReadings.put(sensor2.getId(), new ArrayList<>());
        sensorReadings.put(sensor3.getId(), new ArrayList<>());
        sensorReadings.put(sensor4.getId(), new ArrayList<>());
    }

    // ==================== Room Operations ====================

    public static Map<String, Room> getRooms() {
        return rooms;
    }

    public static Room getRoom(String id) {
        return rooms.get(id);
    }

    public static void addRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public static Room removeRoom(String id) {
        return rooms.remove(id);
    }

    // ==================== Sensor Operations ====================

    public static Map<String, Sensor> getSensors() {
        return sensors;
    }

    public static Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public static void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
    }

    public static Sensor removeSensor(String id) {
        return sensors.remove(id);
    }

    // ==================== Reading Operations ====================

    public static Map<String, List<SensorReading>> getSensorReadings() {
        return sensorReadings;
    }

    public static List<SensorReading> getReadingsForSensor(String sensorId) {
        return sensorReadings.getOrDefault(sensorId, new ArrayList<>());
    }

    public static void addReading(String sensorId, SensorReading reading) {
        sensorReadings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }
}
