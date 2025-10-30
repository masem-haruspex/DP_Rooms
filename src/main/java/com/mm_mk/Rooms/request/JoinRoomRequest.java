package com.mm_mk.Rooms.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record JoinRoomRequest(
        @Schema(description = "User ID joining the room", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotBlank(message = "User ID is required")
        String userId,

        @Schema(description = "Password for private rooms", example = "mysecret123")
        String password
) {
    @JsonCreator
    public JoinRoomRequest(
            @JsonProperty("userId") String userId,
            @JsonProperty("password") String password
    ) {
        this.userId = userId;
        this.password = password;
    }

    public UUID getUserIdAsUUID() {
        return UUID.fromString(userId);
    }
}
