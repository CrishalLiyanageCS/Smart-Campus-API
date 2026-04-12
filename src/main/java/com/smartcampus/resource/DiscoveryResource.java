package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discovery Resource - The root entry point of the Smart Campus API.
 * 
 * Provides API metadata including version, contact information,
 * and navigable links to primary resource collections (HATEOAS).
 * 
 * Endpoint: GET /api/v1
 */
@Path("/")
public class DiscoveryResource {

    /**
     * Returns API metadata and links to available resource collections.
     * This follows HATEOAS principles by providing clients with
     * discoverable navigation links within the response.
     *
     * @return JSON object with API info and resource links
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiInfo() {
        Map<String, Object> apiInfo = new LinkedHashMap<>();
        apiInfo.put("name", "Smart Campus API");
        apiInfo.put("version", "1.0");
        apiInfo.put("description", "RESTful API for managing campus rooms, sensors, and sensor readings");

        // Administrative contact details
        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("name", "Smart Campus Administrator");
        contact.put("email", "admin@smartcampus.westminster.ac.uk");
        apiInfo.put("contact", contact);

        // Navigable resource links (HATEOAS)
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        apiInfo.put("resources", resources);

        return Response.ok(apiInfo).build();
    }
}
