// src/main/java/com/mm_mk/Rooms/request/JoinRoomRequest.java
package com.mm_mk.Rooms.request;

import java.util.UUID;

public record JoinRoomRequest(
        UUID userId
) {}
