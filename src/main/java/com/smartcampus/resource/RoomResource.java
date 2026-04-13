package com.smartcampus.resource;

import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.repository.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Room Resource — Full CRUD operations for campus rooms.
 *
 * Endpoints:
 *   GET    /api/v1/rooms          — List all rooms
 *   GET    /api/v1/rooms/{roomId} — Get a single room by ID
 *   POST   /api/v1/rooms          — Create a new room
 *   PUT    /api/v1/rooms/{roomId} — Update an existing room
 *   DELETE /api/v1/rooms/{roomId} — Delete a room (blocked if sensors are attached)
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    // ==================== GET all rooms ====================

    /**
     * Retrieves all rooms currently stored in the system.
     *
     * @return 200 OK with a JSON array of Room objects
     */
    @GET
    public Response getAllRooms() {
        return Response.ok(new ArrayList<>(DataStore.getRooms().values())).build();
    }

    // ==================== GET single room by ID ====================

    /**
     * Retrieves a single room by its unique identifier.
     *
     * @param roomId the unique room ID (e.g., "LIB-301")
     * @return 200 OK with the Room object, or 404 Not Found
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);
        if (room == null) {
            throw new ResourceNotFoundException(
                    "Room with ID '" + roomId + "' was not found.");
        }
        return Response.ok(room).build();
    }

    // ==================== POST — create a new room ====================

    /**
     * Creates a new room in the system.
     * Validates that the room name is not null/empty and capacity is greater than 0.
     *
     * @param room the Room object from the JSON request body
     * @return 201 Created with Location header, or 400 Bad Request on validation failure
     */
    @POST
    public Response createRoom(Room room) {
        // --- Input Validation ---
        if (room.getName() == null || room.getName().trim().isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "VALIDATION_ERROR");
            error.put("message", "Room name cannot be null or empty.");
            error.put("status", 400);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        if (room.getCapacity() <= 0) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "VALIDATION_ERROR");
            error.put("message", "Room capacity must be greater than 0.");
            error.put("status", 400);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // Generate an ID if the client didn't supply one
        if (room.getId() == null || room.getId().trim().isEmpty()) {
            room.setId("ROOM-" + System.currentTimeMillis());
        }

        // Ensure sensorIds list is initialized
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        // Persist the new room
        DataStore.addRoom(room);

        // Return 201 Created with the Location header pointing to the new resource
        URI location = URI.create("/api/v1/rooms/" + room.getId());
        return Response.created(location).entity(room).build();
    }

    // ==================== PUT — update an existing room ====================

    /**
     * Updates an existing room's name and/or capacity.
     * The room must already exist in the system.
     *
     * @param roomId      the ID of the room to update
     * @param updatedRoom the Room object containing the new values
     * @return 200 OK with the updated Room, or 404 Not Found
     */
    @PUT
    @Path("/{roomId}")
    public Response updateRoom(@PathParam("roomId") String roomId, Room updatedRoom) {
        Room existingRoom = DataStore.getRoom(roomId);
        if (existingRoom == null) {
            throw new ResourceNotFoundException(
                    "Room with ID '" + roomId + "' was not found.");
        }

        // Update fields if provided
        if (updatedRoom.getName() != null && !updatedRoom.getName().trim().isEmpty()) {
            existingRoom.setName(updatedRoom.getName());
        }
        if (updatedRoom.getCapacity() > 0) {
            existingRoom.setCapacity(updatedRoom.getCapacity());
        }

        return Response.ok(existingRoom).build();
    }

    // ==================== DELETE — remove a room ====================

    /**
     * Deletes a room by its ID.
     *
     * Business rule: A room CANNOT be deleted if it still has sensors assigned.
     * This prevents orphaned sensor records and data integrity issues.
     *
     * @param roomId the ID of the room to delete
     * @return 204 No Content on success,
     *         404 Not Found if room doesn't exist,
     *         409 Conflict if room has sensors attached
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);
        if (room == null) {
            throw new ResourceNotFoundException(
                    "Room with ID '" + roomId + "' was not found.");
        }

        // Business rule: block deletion if room has assigned sensors
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room " + roomId + " cannot be deleted: "
                            + room.getSensorIds().size() + " active sensor(s) assigned.");
        }

        // Safe to delete — no sensors linked
        DataStore.removeRoom(roomId);
        return Response.noContent().build();
    }
}
