// src/main/java/com/mm_mk/Rooms/service/RoomService.java
package com.mm_mk.Rooms.service;

import com.mm_mk.Rooms.model.Room;
import com.mm_mk.Rooms.model.RoomParticipant;
import com.mm_mk.Rooms.repository.RoomRepository;
import com.mm_mk.Rooms.repository.RoomParticipantRepository;
import com.mm_mk.Rooms.response.RoomResponse;
import com.mm_mk.Rooms.response.JoinRoomResponse;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.rooms}")
    private String roomsExchange;

    @Value("${rabbitmq.routingkey.room.created}")
    private String roomCreatedRoutingKey;

    @Value("${rabbitmq.routingkey.user.joined}")
    private String userJoinedRoutingKey;

    @Value("${rabbitmq.routingkey.user.left}")
    private String userLeftRoutingKey;

    @Value("${rabbitmq.routingkey.user.kicked}")
    private String userKickedRoutingKey;

    @Value("${rabbitmq.routingkey.user.muted}")
    private String userMutedRoutingKey;

    @Value("${rabbitmq.routingkey.room.deleted}")
    private String roomDeletedRoutingKey;

    public RoomService(RoomRepository roomRepository,
                       RoomParticipantRepository participantRepository,
                       RabbitTemplate rabbitTemplate) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public RoomResponse createRoom(UUID ownerId, String name, Boolean isPrivate, Integer maxParticipants) {
        UUID roomId = UUID.randomUUID();
        String code = generateRoomCode();

        Room room = Room.builder()
                .id(roomId)
                .code(code)
                .name(name)
                .ownerId(ownerId)
                .isPrivate(isPrivate != null ? isPrivate : false)
                .maxParticipants(maxParticipants != null ? maxParticipants : 2)
                .createdAt(LocalDateTime.now())
                .build();

        room = roomRepository.save(room);

        rabbitTemplate.convertAndSend(roomsExchange, roomCreatedRoutingKey, room);

        return new RoomResponse(
                room.getId(),
                room.getCode(),
                room.getName(),
                room.getOwnerId(),
                room.getIsPrivate(),
                room.getMaxParticipants(),
                room.getCreatedAt()
        );
    }

    @Transactional
    public JoinRoomResponse joinRoom(String roomCode, UUID userId) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (participantRepository.existsByRoomIdAndUserId(room.getId(), userId)) {
            throw new RuntimeException("User already in room");
        }

        long participantCount = participantRepository.findByRoomId(room.getId()).size();
        if (participantCount >= room.getMaxParticipants()) {
            throw new RuntimeException("Room is full");
        }

        UUID participantId = UUID.randomUUID();
        RoomParticipant participant = RoomParticipant.builder()
                .id(participantId)
                .roomId(room.getId())
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build();

        participant = participantRepository.save(participant);

        rabbitTemplate.convertAndSend(roomsExchange, userJoinedRoutingKey, participant);

        return new JoinRoomResponse(
                participant.getRoomId(),
                participant.getUserId(),
                participant.getJoinedAt()
        );
    }

    public RoomResponse getRoomByCode(String code) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return new RoomResponse(
                room.getId(),
                room.getCode(),
                room.getName(),
                room.getOwnerId(),
                room.getIsPrivate(),
                room.getMaxParticipants(),
                room.getCreatedAt()
        );
    }

    private String generateRoomCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
    // Room deleted
    @Transactional
    public void deleteRoom(UUID roomId, UUID ownerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Only owner can delete room");
        }

        roomRepository.deleteById(roomId);
        participantRepository.deleteByRoomId(roomId); // cleanup

        rabbitTemplate.convertAndSend(
                roomsExchange,
                roomDeletedRoutingKey,
                Map.of("roomId", roomId)
        );
    }

    @Transactional
    public void leaveRoom(String roomCode, UUID userId) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!participantRepository.existsByRoomIdAndUserId(room.getId(), userId)) {
            throw new RuntimeException("User not in room");
        }

        participantRepository.deleteByRoomIdAndUserId(room.getId(), userId);

        rabbitTemplate.convertAndSend(
                roomsExchange,
                userLeftRoutingKey,
                Map.of("roomId", room.getId(), "userId", userId)
        );
    }

    @Transactional
    public void kickUser(String roomCode, UUID ownerId, UUID userIdToKick) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Only owner can kick");
        }

        if (userIdToKick.equals(ownerId)) {
            throw new RuntimeException("Owner cannot kick themselves");
        }

        participantRepository.deleteByRoomIdAndUserId(room.getId(), userIdToKick);

        rabbitTemplate.convertAndSend(
                roomsExchange,
                userKickedRoutingKey,
                Map.of(
                        "roomId", room.getId(),
                        "userId", userIdToKick,
                        "kickedBy", ownerId
                )
        );
    }

    @Transactional
    public void muteUser(String roomCode, UUID ownerId, UUID userIdToMute) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Only owner can mute");
        }

        rabbitTemplate.convertAndSend(
                roomsExchange,
                userMutedRoutingKey,
                Map.of(
                        "roomId", room.getId(),
                        "userId", userIdToMute,
                        "mutedBy", ownerId
                )
        );
    }
}
