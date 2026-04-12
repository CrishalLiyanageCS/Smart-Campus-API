package com.smartcampus;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * JAX-RS Application configuration class for the Smart Campus API.
 * 
 * This class extends ResourceConfig (which is a subclass of javax.ws.rs.core.Application)
 * and uses the @ApplicationPath annotation to define the versioned API base path.
 * 
 * The packages() method tells Jersey to scan the com.smartcampus package
 * for all resource classes, exception mappers, and filters automatically.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Scan the entire com.smartcampus package tree for:
        // - @Path annotated resource classes
        // - @Provider annotated exception mappers and filters
        packages("com.smartcampus");
    }
}
