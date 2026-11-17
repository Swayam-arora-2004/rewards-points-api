package com.swayam.rewards.exception;

/**
 * Thrown when the requested redemption exceeds the member's current balance.
 * Maps to HTTP 409 (Conflict) in GlobalExceptionHandler.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(long requested, long available) {
        super("Insufficient balance: requested " + requested + " but only " + available + " available");
    }
}

