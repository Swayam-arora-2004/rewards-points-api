package com.swayam.rewards.dto;

import jakarta.validation.constraints.Positive;

/**
 * Request body for POST /members/{id}/earn and POST /members/{id}/redeem.
 */
public record PointsRequest(
        @Positive(message = "points must be greater than 0")
        long points
) {}

