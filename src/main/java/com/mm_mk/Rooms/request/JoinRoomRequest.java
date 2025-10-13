package com.mm_mk.Rooms.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record JoinRoomRequest(
    @NotBlank(message = "User ID is required")
    String userId,

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
