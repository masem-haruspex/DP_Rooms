package com.mm_mk.Rooms.request;

import jakarta.validation.constraints.*;

public record CreateRoomRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be under 100 characters")
        String name,

        Boolean isPrivate,

        String password,

        @Min(value = 2, message = "Min 2 participants")
        @Max(value = 10, message = "Max 10 participants")
        Integer maxParticipants
) {}