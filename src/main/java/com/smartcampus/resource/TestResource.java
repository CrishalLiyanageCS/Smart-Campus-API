package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Temporary test endpoint to verify the GenericExceptionMapper (500 safety net).
 */
@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {

    @GET
    @Path("/500")
    public Response triggerInternalError() {
        // Deliberately throw an unchecked exception to test the safety net
        throw new RuntimeException("Simulated unexpected server error for testing");
    }
}
