package com.smartcampus.exception;

/**
 * Thrown when a requested resource (Room, Sensor, etc.) cannot be found
 * in the data store by its ID.
 *
 * Mapped to HTTP 404 Not Found by ResourceNotFoundExceptionMapper.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
