// src/main/java/com/mm_mk/Rooms/request/KickUserRequest.java
package com.mm_mk.Rooms.request;

import java.util.UUID;

public record KickUserRequest(
        UUID userId
) {}
