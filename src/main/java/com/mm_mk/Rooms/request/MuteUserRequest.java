package com.mm_mk.Rooms.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record MuteUserRequest(
        @NotBlank
        UUID userId
) {}