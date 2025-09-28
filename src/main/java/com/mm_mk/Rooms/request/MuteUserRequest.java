// src/main/java/com/mm_mk/Rooms/request/MuteUserRequest.java
package com.mm_mk.Rooms.request;

import java.util.UUID;

public record MuteUserRequest(
        UUID userId
) {}