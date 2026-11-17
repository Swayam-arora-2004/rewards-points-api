package com.swayam.rewards.exception;

/**
 * Thrown when a POST /members request uses an email address already registered.
 * Maps to HTTP 409 (Conflict) in GlobalExceptionHandler.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}

