package com.smartcampus.exception.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic catch-all exception mapper for any unhandled exceptions.
 *
 * Maps all uncaught Throwables to a 500 Internal Server Error response.
 * IMPORTANT: Never exposes stack traces or internal details to the client
 * for security reasons. The full exception is logged server-side only.
 *
 * Returns a consistent JSON error body:
 * {
 *   "error": "INTERNAL_SERVER_ERROR",
 *   "message": "An unexpected error occurred. Please try again later.",
 *   "status": 500
 * }
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Let Jersey handle its own routing exceptions (404, 405 etc.) normally
        if (exception instanceof WebApplicationException) {
            return ((WebApplicationException) exception).getResponse();
        }

        // Log unexpected errors server-side only — never expose to client
        LOGGER.log(Level.SEVERE, "Unhandled exception caught: " + exception.getMessage(), exception);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", "INTERNAL_SERVER_ERROR");
        error.put("message", "An unexpected error occurred. Please try again later.");
        error.put("status", 500);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}