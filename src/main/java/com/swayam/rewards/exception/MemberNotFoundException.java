package com.swayam.rewards.exception;

/**
 * Thrown when a requested member does not exist in the database.
 * Maps to HTTP 404 in GlobalExceptionHandler.
 */
public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(Long id) {
        super("Member not found: " + id);
    }
}

