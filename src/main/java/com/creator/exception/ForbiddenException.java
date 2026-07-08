package com.creator.exception;

/**
 * Thrown when a user attempts an action their role does not permit.
 * For example, a 'member' trying to invite a user, or an 'admin'
 * trying to change the agency's billing plan.
 * Maps to HTTP 403 (Forbidden).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String action, String role) {
        super(String.format("Forbidden: '%s' role is not allowed to %s", role, action));
    }
}
