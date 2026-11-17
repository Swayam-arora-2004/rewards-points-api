package com.swayam.rewards.dto;

import java.util.List;

/**
 * Response for GET /members/{id} – balance plus full transaction history newest-first.
 */
public record MemberResponse(
        Long id,
        String name,
        String email,
        long balance,
        List<TransactionResponse> transactions
) {}
