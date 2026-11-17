package com.swayam.rewards.dto;

import com.swayam.rewards.model.PointsTransaction;
import java.time.Instant;

/**
 * One entry in the transaction history list returned by GET /members/{id}.
 */
public record TransactionResponse(
        Long id,
        String type,
        long points,
        Instant createdAt
) {
    public static TransactionResponse from(PointsTransaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getType().name(),
                tx.getPoints(),
                tx.getCreatedAt()
        );
    }
}

