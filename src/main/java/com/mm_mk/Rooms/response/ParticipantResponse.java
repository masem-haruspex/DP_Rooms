package com.mm_mk.Rooms.response;

import java.util.UUID;

public record ParticipantResponse(
        UUID id,
        String username,
        UUID userId,
        String preferredKeyboard,
        java.time.LocalDateTime joinedAt
) {}