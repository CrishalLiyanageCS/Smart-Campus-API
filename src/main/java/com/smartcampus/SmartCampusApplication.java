package com.smartcampus;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // Resource classes
        classes.add(com.smartcampus.resource.DiscoveryResource.class);
        classes.add(com.smartcampus.resource.RoomResource.class);
        classes.add(com.smartcampus.resource.SensorResource.class);
        classes.add(com.smartcampus.resource.TestResource.class);

        // Exception mappers
        classes.add(com.smartcampus.exception.mapper.GenericExceptionMapper.class);
        classes.add(com.smartcampus.exception.mapper.LinkedResourceNotFoundExceptionMapper.class);
        classes.add(com.smartcampus.exception.mapper.ResourceNotFoundExceptionMapper.class);
        classes.add(com.smartcampus.exception.mapper.RoomNotEmptyExceptionMapper.class);
        classes.add(com.smartcampus.exception.mapper.SensorUnavailableExceptionMapper.class);

        // Filters
        classes.add(com.smartcampus.filter.LoggingFilter.class);

        return classes;
    }
}