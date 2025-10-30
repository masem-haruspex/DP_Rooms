package com.mm_mk.Rooms.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record KickUserRequest(
        @Schema(description = "User ID to kick", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotBlank
        UUID userId
) {}
