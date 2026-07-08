package com.creator.exception;

/**
 * Thrown when a request contains invalid data, such as duplicate emails
 * or conflicting link operations.
 * Maps to HTTP 409 (Conflict) or HTTP 400 (Bad Request) depending on context.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
