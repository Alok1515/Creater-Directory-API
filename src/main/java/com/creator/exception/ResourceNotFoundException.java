package com.creator.exception;

/**
 * Thrown when a requested resource cannot be found, or when a tenant
 * does not have access to the resource (returns 404 to avoid leaking
 * existence information across tenants).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
