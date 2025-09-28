package com.mm_mk.Rooms.response;

import java.util.UUID;

public record JoinRoomResponse(
        UUID roomId,
        UUID userId,
        java.time.LocalDateTime joinedAt
) {}