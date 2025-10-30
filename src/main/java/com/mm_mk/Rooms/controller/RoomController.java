package com.mm_mk.Rooms.controller;

import com.mm_mk.Rooms.request.*;
import com.mm_mk.Rooms.response.JoinRoomResponse;
import com.mm_mk.Rooms.response.ParticipantResponse;
import com.mm_mk.Rooms.response.RoomResponse;
import com.mm_mk.Rooms.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms", description = "Room management and participant operations")
public class RoomController {

    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);
    private static final long SLOW_OPERATION_THRESHOLD_MS = 2000;

    @Autowired
    private RoomService roomService;

    @PostMapping
    @Operation(summary = "Create a new room", description = "Create a chat room with privacy settings and participant limits")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "201", description = "Room created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    public ResponseEntity<RoomResponse> createRoom(@RequestHeader("X-User-ID") UUID ownerId, @Valid @RequestBody CreateRoomRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("CREATE_ROOM request - ownerId: {},isPrivate: {}, maxParticipants: {}", ownerId, request.isPrivate(), request.maxParticipants());

        try {
            RoomResponse room = roomService.createRoom(
                    ownerId,
                    request.isPrivate(),
                    request.password(),
                    request.maxParticipants()
            );

            logger.info("CREATE_ROOM success - roomId: {}, code: {}, ownerId: {}", room.id(), room.code(), ownerId);
            return ResponseEntity.status(201).body(room);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("CREATE_ROOM API call completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS)
                logger.warn("SLOW API: CREATE_ROOM took {}ms", duration);
        }
    }

    @PostMapping("/{code}/join")
    @Operation(summary = "Join a room", description = "Join an existing room, password required for private rooms")
    @ApiResponse(responseCode = "200", description = "Successfully joined room")
    @ApiResponse(responseCode = "400", description = "Invalid room code or password")
    @ApiResponse(responseCode = "403", description = "Room is full or access denied")
    public ResponseEntity<JoinRoomResponse> joinRoom(@PathVariable String code, @Valid @RequestBody JoinRoomRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("JOIN_ROOM request - roomCode: {}, userId: {}", code, request.userId());

        try {
            JoinRoomResponse response = roomService.joinRoom(code, request.getUserIdAsUUID(), request.password());
            logger.info("JOIN_ROOM success - roomCode: {}, userId: {}, roomId: {}", code, request.userId(), response.roomId());
            return ResponseEntity.ok(response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("JOIN_ROOM API call completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS)
                logger.warn("SLOW API: JOIN_ROOM took {}ms", duration);
        }
    }

    @PostMapping("/{code}/leave")
    @Operation(summary = "Leave a room", description = "Leave a room you're currently participating in")
    @ApiResponse(responseCode = "204", description = "Successfully left room")
    @ApiResponse(responseCode = "404", description = "Room or user not found")
    public ResponseEntity<Void> leaveRoom(@PathVariable String code, @RequestHeader("X-User-ID") UUID userId) {
        long startTime = System.currentTimeMillis();
        logger.info("LEAVE_ROOM request - roomCode: {}, userId: {}", code, userId);

        try {
            roomService.leaveRoom(code, userId);
            logger.info("LEAVE_ROOM success - roomCode: {}, userId: {}", code, userId);
            return ResponseEntity.noContent().build();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("LEAVE_ROOM API call completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS)
                logger.warn("SLOW API: LEAVE_ROOM took {}ms", duration);
        }
    }

    @PostMapping("/{code}/kick")
    @Operation(summary = "Kick user from room", description = "Room owner can remove a participant from the room")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "User kicked successfully")
    @ApiResponse(responseCode = "403", description = "Only room owner can kick users")
    @ApiResponse(responseCode = "404", description = "Room or user not found")
    public ResponseEntity<Void> kickUser(@PathVariable String code, @RequestHeader("X-User-ID") UUID ownerId, @Valid @RequestBody KickUserRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("KICK_USER request - roomCode: {}, ownerId: {}, targetUserId: {}", code, ownerId, request.userId());

        try {
            roomService.kickUser(code, ownerId, request.userId());
            logger.info("KICK_USER success - roomCode: {}, ownerId: {}, targetUserId: {}", code, ownerId, request.userId());
            return ResponseEntity.noContent().build();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("KICK_USER API call completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS)
                logger.warn("SLOW API: KICK_USER took {}ms", duration);
        }
    }

    @DeleteMapping("/{roomId}")
    @Operation(summary = "Delete room", description = "Room owner can permanently delete a room")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "Room deleted successfully")
    @ApiResponse(responseCode = "403", description = "Only room owner can delete room")
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID roomId, @RequestHeader("X-User-ID") UUID ownerId) {
        long startTime = System.currentTimeMillis();
        logger.info("DELETE_ROOM request - roomId: {}, ownerId: {}", roomId, ownerId);

        try {
            roomService.deleteRoom(roomId, ownerId);
            logger.info("DELETE_ROOM success - roomId: {}, ownerId: {}", roomId, ownerId);
            return ResponseEntity.noContent().build();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("DELETE_ROOM API call completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS)
                logger.warn("SLOW API: DELETE_ROOM took {}ms", duration);
        }
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get room details", description = "Retrieve room information by access code")
    @ApiResponse(responseCode = "200", description = "Room details retrieved")
    @ApiResponse(responseCode = "404", description = "Room not found")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String code) {
        long startTime = System.currentTimeMillis();
        logger.debug("GET_ROOM request - code: {}", code);

        try {
            RoomResponse room = roomService.getRoomByCode(code);
            logger.debug("GET_ROOM success - code: {}, roomId: {}", code, room.id());
            return ResponseEntity.ok(room);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("GET_ROOM API call completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS)
                logger.warn("SLOW API: GET_ROOM took {}ms", duration);
        }
    }

    @GetMapping("/{code}/participants")
    @Operation(summary = "Get room participants", description = "List all participants in a room")
    @ApiResponse(responseCode = "200", description = "Participants list retrieved")
    @ApiResponse(responseCode = "404", description = "Room not found")
    public ResponseEntity<List<ParticipantResponse>> getRoomParticipants(@PathVariable String code) {
        long startTime = System.currentTimeMillis();
        logger.debug("GET_PARTICIPANTS request - roomCode: {}", code);

        try {
            List<ParticipantResponse> participants = roomService.getRoomParticipants(code);
            logger.debug("GET_PARTICIPANTS success - roomCode: {}, participantCount: {}", code, participants.size());
            return ResponseEntity.ok(participants);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("GET_PARTICIPANTS API call completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS)
                logger.warn("SLOW API: GET_PARTICIPANTS took {}ms", duration);
        }
    }
}