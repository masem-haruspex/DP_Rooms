package com.mm_mk.Rooms.response;

import java.util.UUID;

public record RoomResponse(
        UUID id,
        String code,
        String name,
        UUID ownerId,
        Boolean isPrivate,
        Integer maxParticipants,
        java.time.LocalDateTime createdAt
) {}