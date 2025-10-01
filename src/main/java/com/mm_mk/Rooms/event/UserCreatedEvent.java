// src/main/java/com/mm_mk/Rooms/event/UserCreatedEvent.java
package com.mm_mk.Rooms.event;

import java.util.UUID;

public record UserCreatedEvent(
        UUID id,
        String username,
        String email
) {}
