package com.smartcampus.exception;

/**
 * Thrown when attempting to post a reading to a sensor that is
 * currently in MAINTENANCE status and cannot accept new data.
 *
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {

    public SensorUnavailableException(String message) {
        super(message);
    }
}
