package com.smartcampus.exception;

/**
 * Thrown when attempting to delete a room that still has sensors assigned to it.
 * This enforces the business rule that rooms cannot be removed while sensors
 * are actively linked, preventing orphaned sensor records.
 *
 * Mapped to HTTP 409 Conflict by RoomNotEmptyExceptionMapper.
 */
public class RoomNotEmptyException extends RuntimeException {

    public RoomNotEmptyException(String message) {
        super(message);
    }
}
