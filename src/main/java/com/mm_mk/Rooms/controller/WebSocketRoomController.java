package com.mm_mk.Rooms.controller;

import com.mm_mk.Rooms.model.Room;
import com.mm_mk.Rooms.model.RoomParticipant;
import com.mm_mk.Rooms.repository.RoomRepository;
import com.mm_mk.Rooms.repository.RoomParticipantRepository;
import com.mm_mk.Rooms.repository.LocalUserRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
public class WebSocketRoomController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final LocalUserRepository localUserRepository;

    public WebSocketRoomController(SimpMessagingTemplate messagingTemplate,
                                  RoomRepository roomRepository,
                                  RoomParticipantRepository participantRepository,
                                  LocalUserRepository localUserRepository) {
        this.messagingTemplate = messagingTemplate;
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.localUserRepository = localUserRepository;
    }

    @MessageMapping("/rooms/{roomCode}/keyEvent")
    public void handleKeyEvent(@DestinationVariable String roomCode,
                              Map<String, Object> keyEvent,
                              @Header("X-User-ID") UUID userId) {
        messagingTemplate.convertAndSend(
            "/topic/rooms/" + roomCode + "/keyEvents",
            Map.of(
                "userId", userId.toString(),
                "keyEvent", keyEvent,
                "timestamp", System.currentTimeMillis()
            )
        );
    }

    @MessageMapping("/rooms/{roomCode}/sendMessage")
    public void handleChatMessage(@DestinationVariable String roomCode,
                                 Map<String, Object> chatMessage,
                                 @Header("X-User-ID") UUID userId) {
        messagingTemplate.convertAndSend(
            "/topic/rooms/" + roomCode + "/chat",
            Map.of(
                "userId", userId.toString(),
                "message", chatMessage.get("content"),
                "timestamp", System.currentTimeMillis()
            )
        );
    }

    @MessageMapping("/rooms/{roomCode}/join")
    public void handleUserJoin(@DestinationVariable String roomCode,
                              @Header("X-User-ID") UUID userId) {
        localUserRepository.findById(userId).ifPresent(user -> {
            messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomCode + "/participants",
                Map.of(
                    "type", "USER_JOINED",
                    "userId", userId.toString(),
                    "username", user.getUsername(),
                    "timestamp", System.currentTimeMillis()
                )
            );
        });
    }

    @MessageMapping("/rooms/{roomCode}/leave")
    public void handleUserLeave(@DestinationVariable String roomCode,
                               @Header("X-User-ID") UUID userId) {
        messagingTemplate.convertAndSend(
            "/topic/rooms/" + roomCode + "/participants",
            Map.of(
                "type", "USER_LEFT",
                "userId", userId.toString(),
                "timestamp", System.currentTimeMillis()
            )
        );
    }
}
