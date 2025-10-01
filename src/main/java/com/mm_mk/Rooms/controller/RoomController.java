// src/main/java/com/mm_mk/Rooms/controller/RoomController.java
package com.mm_mk.Rooms.controller;

import com.mm_mk.Rooms.request.*;
import com.mm_mk.Rooms.response.JoinRoomResponse;
import com.mm_mk.Rooms.response.RoomResponse;
import com.mm_mk.Rooms.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    /**
     * Create a new room with the given owner.
     */
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @RequestHeader("X-User-ID") UUID ownerId,
            @Valid @RequestBody CreateRoomRequest request) {
        RoomResponse room = roomService.createRoom(
                ownerId,
                request.name(),
                request.isPrivate(),
                request.maxParticipants()
        );
        return ResponseEntity.status(201).body(room);
    }

    /**
     * Join a room using its human-friendly code.
     */
    @PostMapping("/{code}/join")
    public ResponseEntity<JoinRoomResponse> joinRoom(
            @PathVariable String code,
            @Valid @RequestBody JoinRoomRequest request) {
        JoinRoomResponse response = roomService.joinRoom(code, request.userId());
        return ResponseEntity.ok(response);
    }

    /**
     * Leave a room.
     */
    @PostMapping("/{code}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable String code,
            @RequestHeader("X-User-ID") UUID userId) {
        roomService.leaveRoom(code, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Kick another user from the room. Only the owner can do this.
     */
    @PostMapping("/{code}/kick")
    public ResponseEntity<Void> kickUser(
            @PathVariable String code,
            @RequestHeader("X-User-ID") UUID ownerId,
            @Valid @RequestBody KickUserRequest request) {
        roomService.kickUser(code, ownerId, request.userId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Mute another user in the room. Only the owner can do this.
     */
    @PostMapping("/{code}/mute")
    public ResponseEntity<Void> muteUser(
            @PathVariable String code,
            @RequestHeader("X-User-ID") UUID ownerId,
            @Valid @RequestBody MuteUserRequest request) {
        roomService.muteUser(code, ownerId, request.userId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete a room entirely.
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable UUID roomId,
            @RequestHeader("X-User-ID") UUID ownerId) {
        roomService.deleteRoom(roomId, ownerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Fetch room details by its human-friendly code.
     */
    @GetMapping("/{code}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String code) {
        RoomResponse room = roomService.getRoomByCode(code);
        return ResponseEntity.ok(room);
    }
}
