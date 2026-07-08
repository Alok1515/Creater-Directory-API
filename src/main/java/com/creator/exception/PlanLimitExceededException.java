package com.creator.exception;

/**
 * Thrown when a free-plan agency attempts to link more than 5 creators.
 * Maps to HTTP 402 (Payment Required) to signal the caller needs to
 * upgrade their plan.
 */
public class PlanLimitExceededException extends RuntimeException {

    public PlanLimitExceededException(String message) {
        super(message);
    }

    public PlanLimitExceededException() {
        super("Free plan limit reached: maximum 5 creators allowed. Upgrade to pro for unlimited creators.");
    }
}
