package com.mm_mk.Rooms.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record CreateRoomRequest(

        @Schema(description = "Whether the room is private", example = "false")
        Boolean isPrivate,

        @Schema(description = "Password for private rooms", example = "mysecret123")
        String password,

        @Schema(description = "Maximum participants allowed", example = "5", minimum = "2", maximum = "10")
        @Min(value = 2, message = "Min 2 participants")
        @Max(value = 10, message = "Max 10 participants")
        Integer maxParticipants
) {}