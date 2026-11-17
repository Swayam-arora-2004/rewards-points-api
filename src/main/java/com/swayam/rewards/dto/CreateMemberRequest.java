package com.swayam.rewards.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /members.
 * Uses Java record – immutable, no boilerplate (Coding Standard).
 */
public record CreateMemberRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 100, message = "name must be 100 characters or fewer")
        String name,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid address")
        @Size(max = 200, message = "email must be 200 characters or fewer")
        String email
) {}
