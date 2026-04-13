package com.smartcampus.resource;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Sub-Resource for Sensor Readings.
 *
 * This class does NOT have a @Path annotation at the class level because
 * it is managed as a sub-resource via a locator method in SensorResource.
 * Jersey creates instances of this class through the sub-resource locator pattern.
 *
 * Endpoints (relative to parent):
 *   GET  /api/v1/sensors/{sensorId}/readings       — Get all readings for a sensor
 *   POST /api/v1/sensors/{sensorId}/readings       — Add a new reading to a sensor
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    /**
     * Constructor receives the parent sensor ID from the sub-resource locator.
     *
     * @param sensorId the ID of the parent sensor
     */
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ==================== GET all readings for a sensor ====================

    /**
     * Retrieves all readings recorded for the parent sensor.
     *
     * @return 200 OK with a JSON array of SensorReading objects,
     *         or 404 if the parent sensor does not exist
     */
    @GET
    public Response getAllReadings() {
        // Validate that the parent sensor exists
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException(
                    "Sensor with ID '" + sensorId + "' was not found.");
        }

        List<SensorReading> readings = DataStore.getReadingsForSensor(sensorId);
        return Response.ok(readings).build();
    }

    // ==================== POST — add a new reading ====================

    /**
     * Records a new sensor reading for the parent sensor.
     *
     * Business rules:
     *   - The parent sensor must exist (404 if not)
     *   - The parent sensor must NOT be in MAINTENANCE status (403 if it is)
     *   - A UUID is auto-generated for the reading ID
     *   - The timestamp is set to the current epoch time in milliseconds
     *   - The parent sensor's currentValue is updated with this reading's value
     *
     * @param reading the SensorReading object from the JSON request body
     * @return 201 Created with the new reading,
     *         404 if sensor not found,
     *         403 if sensor is in MAINTENANCE
     */
    @POST
    public Response addReading(SensorReading reading) {
        // Validate that the parent sensor exists
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException(
                    "Sensor with ID '" + sensorId + "' was not found.");
        }

        // Business rule: block readings for sensors under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is currently under MAINTENANCE and cannot accept readings.");
        }

        // Auto-generate a UUID for the reading ID
        reading.setId(UUID.randomUUID().toString());

        // Auto-set the timestamp to current time
        reading.setTimestamp(System.currentTimeMillis());

        // Persist the reading
        DataStore.addReading(sensorId, reading);

        // Side effect: update the parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        // Return 201 Created with Location header
        URI location = URI.create("/api/v1/sensors/" + sensorId + "/readings/" + reading.getId());
        return Response.created(location).entity(reading).build();
    }
}
