package com.smartcampus.exception.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps LinkedResourceNotFoundException to a 422 Unprocessable Entity HTTP response.
 *
 * Returns a consistent JSON error body:
 * {
 *   "error": "UNPROCESSABLE_ENTITY",
 *   "message": "...",
 *   "status": 422
 * }
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", "UNPROCESSABLE_ENTITY");
        error.put("message", exception.getMessage());
        error.put("status", 422);

        return Response.status(422)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
