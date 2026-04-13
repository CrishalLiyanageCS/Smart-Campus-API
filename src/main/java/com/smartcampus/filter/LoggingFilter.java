package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * JAX-RS filter that logs incoming HTTP requests and outgoing responses.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter
 * to intercept the request/response lifecycle at two points:
 *   - On request: logs the HTTP method and request URI
 *   - On response: logs the HTTP status code returned to the client
 *
 * Registered automatically via the @Provider annotation and Jersey's
 * package scanning configured in SmartCampusApplication.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Called before the resource method is invoked.
     * Logs the HTTP method (GET, POST, PUT, DELETE) and the full request URI.
     *
     * @param requestContext the incoming request context
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(">>> REQUEST: " + requestContext.getMethod() + " "
                + requestContext.getUriInfo().getRequestUri());
    }

    /**
     * Called after the resource method has been invoked and the response is ready.
     * Logs the HTTP response status code.
     *
     * @param requestContext  the original request context
     * @param responseContext the outgoing response context
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info("<<< RESPONSE: " + responseContext.getStatus() + " "
                + requestContext.getMethod() + " "
                + requestContext.getUriInfo().getRequestUri());
    }
}
