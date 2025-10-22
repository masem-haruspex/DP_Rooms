package com.mm_mk.Rooms.event;

import java.util.UUID;

public record UserUpdatedEvent(
        UUID id,
        String username,
        String preferredKeyboard
) {}
