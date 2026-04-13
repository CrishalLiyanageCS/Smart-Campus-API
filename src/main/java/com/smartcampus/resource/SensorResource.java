package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sensor Resource — Full CRUD operations for campus sensors with room linking.
 *
 * Endpoints:
 *   GET    /api/v1/sensors              — List all sensors (optionally filtered by type)
 *   GET    /api/v1/sensors/{sensorId}   — Get a single sensor by ID
 *   POST   /api/v1/sensors              — Register a new sensor (must link to existing room)
 *   PUT    /api/v1/sensors/{sensorId}   — Update an existing sensor
 *   DELETE /api/v1/sensors/{sensorId}   — Remove a sensor and unlink from its room
 *
 * Sub-resource:
 *   /api/v1/sensors/{sensorId}/readings — Delegated to SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    // ==================== GET all sensors (with optional type filter) ====================

    /**
     * Retrieves all sensors, optionally filtered by sensor type.
     * Filtering is case-insensitive (e.g., "co2" matches "CO2").
     *
     * @param type optional query parameter to filter by sensor type
     * @return 200 OK with a JSON array of matching Sensor objects
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(DataStore.getSensors().values());

        // Apply type filter if provided (case-insensitive)
        if (type != null && !type.trim().isEmpty()) {
            sensors = sensors.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type.trim()))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensors).build();
    }

    // ==================== GET single sensor by ID ====================

    /**
     * Retrieves a single sensor by its unique identifier.
     *
     * @param sensorId the unique sensor ID (e.g., "TEMP-001")
     * @return 200 OK with the Sensor object, or 404 Not Found
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException(
                    "Sensor with ID '" + sensorId + "' was not found.");
        }
        return Response.ok(sensor).build();
    }

    // ==================== POST — register a new sensor ====================

    /**
     * Registers a new sensor in the system.
     * The sensor's roomId must reference an existing room (cross-link validation).
     * On success, the sensor ID is automatically added to the room's sensorIds list.
     *
     * @param sensor the Sensor object from the JSON request body
     * @return 201 Created with Location header,
     *         400 Bad Request on validation failure,
     *         422 Unprocessable Entity if roomId doesn't exist
     */
    @POST
    public Response createSensor(Sensor sensor) {
        // --- Input Validation ---
        if (sensor.getType() == null || sensor.getType().trim().isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "VALIDATION_ERROR");
            error.put("message", "Sensor type cannot be null or empty.");
            error.put("status", 400);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        if (sensor.getRoomId() == null || sensor.getRoomId().trim().isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "VALIDATION_ERROR");
            error.put("message", "Sensor roomId cannot be null or empty.");
            error.put("status", 400);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // --- Cross-link validation: room must exist ---
        Room room = DataStore.getRoom(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: Room with ID '"
                            + sensor.getRoomId() + "' does not exist.");
        }

        // Generate an ID if the client didn't supply one
        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            sensor.setId("SENSOR-" + System.currentTimeMillis());
        }

        // Set default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }

        // Persist the new sensor
        DataStore.addSensor(sensor);

        // Cross-link: add sensor ID to the room's sensorIds list
        room.getSensorIds().add(sensor.getId());

        // Return 201 Created with the Location header
        URI location = URI.create("/api/v1/sensors/" + sensor.getId());
        return Response.created(location).entity(sensor).build();
    }

    // ==================== PUT — update an existing sensor ====================

    /**
     * Updates an existing sensor's type, status, currentValue, or roomId.
     * If roomId changes, cross-link integrity is maintained by moving
     * the sensor ID from the old room to the new room.
     *
     * @param sensorId      the ID of the sensor to update
     * @param updatedSensor the Sensor object containing the new values
     * @return 200 OK with the updated Sensor, or 404 / 422
     */
    @PUT
    @Path("/{sensorId}")
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updatedSensor) {
        Sensor existingSensor = DataStore.getSensor(sensorId);
        if (existingSensor == null) {
            throw new ResourceNotFoundException(
                    "Sensor with ID '" + sensorId + "' was not found.");
        }

        // If roomId is being changed, validate and update cross-links
        if (updatedSensor.getRoomId() != null && !updatedSensor.getRoomId().trim().isEmpty()
                && !updatedSensor.getRoomId().equals(existingSensor.getRoomId())) {

            Room newRoom = DataStore.getRoom(updatedSensor.getRoomId());
            if (newRoom == null) {
                throw new LinkedResourceNotFoundException(
                        "Cannot move sensor: Room with ID '"
                                + updatedSensor.getRoomId() + "' does not exist.");
            }

            // Remove sensor ID from old room's list
            Room oldRoom = DataStore.getRoom(existingSensor.getRoomId());
            if (oldRoom != null) {
                oldRoom.getSensorIds().remove(sensorId);
            }

            // Add sensor ID to new room's list
            newRoom.getSensorIds().add(sensorId);

            existingSensor.setRoomId(updatedSensor.getRoomId());
        }

        // Update other fields if provided
        if (updatedSensor.getType() != null && !updatedSensor.getType().trim().isEmpty()) {
            existingSensor.setType(updatedSensor.getType());
        }
        if (updatedSensor.getStatus() != null && !updatedSensor.getStatus().trim().isEmpty()) {
            existingSensor.setStatus(updatedSensor.getStatus());
        }
        // currentValue can be 0.0, so always update if PUT body includes it
        existingSensor.setCurrentValue(updatedSensor.getCurrentValue());

        return Response.ok(existingSensor).build();
    }

    // ==================== DELETE — remove a sensor ====================

    /**
     * Removes a sensor from the system and unlinks it from its parent room.
     * This maintains cross-link integrity by removing the sensor ID
     * from the room's sensorIds list.
     *
     * @param sensorId the ID of the sensor to delete
     * @return 204 No Content on success, or 404 Not Found
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException(
                    "Sensor with ID '" + sensorId + "' was not found.");
        }

        // Cross-link: remove sensor ID from the parent room's sensorIds list
        Room room = DataStore.getRoom(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().remove(sensorId);
        }

        // Remove the sensor from the data store
        DataStore.removeSensor(sensorId);

        // Also clean up any readings for this sensor
        DataStore.getSensorReadings().remove(sensorId);

        return Response.noContent().build();
    }

    // ==================== Sub-Resource Locator for Readings ====================

    /**
     * Sub-resource locator method that delegates all requests matching
     * /sensors/{sensorId}/readings to the SensorReadingResource class.
     *
     * This follows the JAX-RS sub-resource locator pattern:
     * - The method is annotated with @Path but has NO HTTP method annotation
     * - It returns an instance of the sub-resource class
     * - Jersey will then match HTTP methods on the returned sub-resource
     *
     * @param sensorId the ID of the parent sensor
     * @return a new SensorReadingResource instance scoped to this sensor
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsSubResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
