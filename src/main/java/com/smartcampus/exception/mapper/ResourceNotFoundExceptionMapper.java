package com.smartcampus.exception.mapper;

import com.smartcampus.exception.ResourceNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps ResourceNotFoundException to a 404 Not Found HTTP response.
 *
 * Returns a consistent JSON error body:
 * {
 *   "error": "NOT_FOUND",
 *   "message": "...",
 *   "status": 404
 * }
 */
@Provider
public class ResourceNotFoundExceptionMapper implements ExceptionMapper<ResourceNotFoundException> {

    @Override
    public Response toResponse(ResourceNotFoundException exception) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", "NOT_FOUND");
        error.put("message", exception.getMessage());
        error.put("status", 404);

        return Response.status(Response.Status.NOT_FOUND)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
