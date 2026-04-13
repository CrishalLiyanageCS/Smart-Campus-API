package com.smartcampus.exception;

/**
 * Thrown when a cross-linked resource reference is invalid.
 * For example, when creating or updating a sensor with a roomId
 * that does not correspond to any existing room.
 *
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
