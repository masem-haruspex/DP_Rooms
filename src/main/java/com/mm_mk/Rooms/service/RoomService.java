package com.mm_mk.Rooms.service;

import com.mm_mk.Rooms.model.LocalUser;
import com.mm_mk.Rooms.model.Room;
import com.mm_mk.Rooms.model.RoomParticipant;
import com.mm_mk.Rooms.repository.LocalUserRepository;
import com.mm_mk.Rooms.repository.RoomRepository;
import com.mm_mk.Rooms.repository.RoomParticipantRepository;
import com.mm_mk.Rooms.response.ParticipantResponse;
import com.mm_mk.Rooms.response.RoomResponse;
import com.mm_mk.Rooms.response.JoinRoomResponse;
import com.mm_mk.Rooms.util.CorrelationIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomService {
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);
    private static final long SLOW_OPERATION_THRESHOLD_MS = 1000;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final LocalUserRepository localUserRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PasswordEncoder passwordEncoder;

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
    @Value("${rabbitmq.routingkey.room.deleted}")
    private String roomDeletedRoutingKey;

    public RoomService(RoomRepository roomRepository,
                      RoomParticipantRepository participantRepository,
                      LocalUserRepository localUserRepository,
                      RabbitTemplate rabbitTemplate,
                      PasswordEncoder passwordEncoder) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.localUserRepository = localUserRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RoomResponse createRoom(UUID ownerId, Boolean isPrivate, String password, Integer maxParticipants) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting room creation - ownerId: {}, name: {}", ownerId);
        try {
            LocalUser owner = localUserRepository.findById(ownerId)
                .orElseThrow(() -> {
                    logger.error("Owner not found in local_users - ownerId: {}", ownerId);
                    return new RuntimeException("Owner not found in local_users");
                });

            UUID roomId = UUID.randomUUID();
            String code = generateRoomCode();
            logger.debug("Generated room - id: {}, code: {}", roomId, code);

            Room room = Room.builder()
                .id(roomId)
                .code(code)
                .owner(owner)
                .isPrivate(isPrivate != null ? isPrivate : false)
                .password(password != null && !password.isEmpty() ? passwordEncoder.encode(password) : null)
                .maxParticipants(maxParticipants != null ? maxParticipants : 2)
                .createdAt(LocalDateTime.now())
                .build();

            room = roomRepository.save(room);
            logger.info("Room created successfully - roomId: {}, code: {}, name: {}", roomId, code);

            RoomParticipant ownerParticipant = RoomParticipant.builder()
                .id(UUID.randomUUID())
                .room(room)
                .user(owner)
                .joinedAt(LocalDateTime.now())
                .build();
            participantRepository.save(ownerParticipant);
            logger.debug("Owner added as participant - roomId: {}, ownerId: {}", roomId, ownerId);

            sendRoomCreatedEvent(room);
            logger.debug("Room creation event sent to RabbitMQ - roomId: {}", roomId);

            return new RoomResponse(
                room.getId(),
                room.getCode(),
                room.getOwner().getId(),
                room.getIsPrivate(),
                null, 
                room.getMaxParticipants(),
                room.getCreatedAt()
            );
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Room creation completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW OPERATION: Room creation took {}ms", duration);
            }
        }
    }

    @Transactional
    public JoinRoomResponse joinRoom(String roomCode, UUID userId, String password) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting room join - roomCode: {}, userId: {}", roomCode, userId);
        try {
            Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> {
                    logger.warn("Room not found - roomCode: {}", roomCode);
                    return new RuntimeException("Room not found");
                });

            LocalUser user = localUserRepository.findById(userId).orElseGet(() -> {
                logger.info("Creating guest user - userId: {}", userId);
                LocalUser guestUser = LocalUser.builder()
                    .id(userId)
                    .username("Guest_" + userId.toString().substring(0, 8))
                    .preferredKeyboard("Casio")
                    .lastSyncedAt(LocalDateTime.now())
                    .build();
                return localUserRepository.save(guestUser);
            });

            if (room.getIsPrivate() && room.getPassword() != null && !room.getPassword().isEmpty()) {
                if (password == null || !passwordEncoder.matches(password, room.getPassword())) {
                    logger.warn("Invalid room password attempt - roomCode: {}, userId: {}", roomCode, userId);
                    throw new RuntimeException("Invalid room password");
                }
                logger.debug("Room password validated successfully - roomCode: {}", roomCode);
            }

            if (participantRepository.existsByRoomAndUser(room, user)) {
                logger.warn("User already in room - roomCode: {}, userId: {}", roomCode, userId);
                throw new RuntimeException("User already in room");
            }

            long participantCount = participantRepository.findByRoom(room).size();
            if (participantCount >= room.getMaxParticipants()) {
                logger.warn("Room is full - roomCode: {}, currentParticipants: {}, maxParticipants: {}",
                    roomCode, participantCount, room.getMaxParticipants());
                throw new RuntimeException("Room is full");
            }

            UUID participantId = UUID.randomUUID();
            RoomParticipant participant = RoomParticipant.builder()
                .id(participantId)
                .room(room)
                .user(user)
                .joinedAt(LocalDateTime.now())
                .build();
            participant = participantRepository.save(participant);
            logger.info("User joined room successfully - roomCode: {}, userId: {}, participantId: {}",
                roomCode, userId, participantId);

            sendUserJoinedEvent(room, user, participant);

            return new JoinRoomResponse(
                participant.getRoom().getId(),
                participant.getUser().getId(),
                participant.getJoinedAt()
            );
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Room join completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW OPERATION: Room join took {}ms", duration);
            }
        }
    }

    @Transactional
    public void leaveRoom(String roomCode, UUID userId) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting room leave - roomCode: {}, userId: {}", roomCode, userId);
        try {
            Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> {
                    logger.warn("Room not found for leave operation - roomCode: {}", roomCode);
                    return new RuntimeException("Room not found");
                });

            LocalUser user = localUserRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found for leave operation - userId: {}", userId);
                    return new RuntimeException("User not found in local_users");
                });

            if (!participantRepository.existsByRoomAndUser(room, user)) {
                logger.warn("User not in room - roomCode: {}, userId: {}", roomCode, userId);
                throw new RuntimeException("User not in room");
            }

            participantRepository.deleteByRoomAndUser(room, user);
            logger.info("User left room successfully - roomCode: {}, userId: {}", roomCode, userId);

            sendUserLeftEvent(room, user);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Room leave completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW OPERATION: Room leave took {}ms", duration);
            }
        }
    }

    @Transactional
    public void kickUser(String roomCode, UUID ownerId, UUID userIdToKick) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting user kick - roomCode: {}, ownerId: {}, userIdToKick: {}",
            roomCode, ownerId, userIdToKick);
        try {
            Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> {
                    logger.warn("Room not found for kick operation - roomCode: {}", roomCode);
                    return new RuntimeException("Room not found");
                });

            if (!room.getOwner().getId().equals(ownerId)) {
                logger.warn("Unauthorized kick attempt - roomCode: {}, attemptedBy: {}, actualOwner: {}",
                    roomCode, ownerId, room.getOwner().getId());
                throw new RuntimeException("Only owner can kick");
            }

            if (userIdToKick.equals(ownerId)) {
                logger.warn("Owner attempted to kick themselves - roomCode: {}, ownerId: {}", roomCode, ownerId);
                throw new RuntimeException("Owner cannot kick themselves");
            }

            LocalUser userToKick = localUserRepository.findById(userIdToKick)
                .orElseThrow(() -> {
                    logger.warn("User to kick not found - userId: {}", userIdToKick);
                    return new RuntimeException("User to kick not found");
                });

            participantRepository.deleteByRoomAndUser(room, userToKick);
            logger.info("User kicked successfully - roomCode: {}, userIdKicked: {}, kickedBy: {}",
                roomCode, userIdToKick, ownerId);

            sendUserKickedEvent(room, userToKick, ownerId);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("User kick completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW OPERATION: User kick took {}ms", duration);
            }
        }
    }

    @Transactional
    public void deleteRoom(UUID roomId, UUID ownerId) {
        long startTime = System.currentTimeMillis();
        logger.info("Starting room deletion - roomId: {}, ownerId: {}", roomId, ownerId);
        try {
            Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> {
                    logger.warn("Room not found for deletion - roomId: {}", roomId);
                    return new RuntimeException("Room not found");
                });

            if (!room.getOwner().getId().equals(ownerId)) {
                logger.warn("Unauthorized room deletion attempt - roomId: {}, attemptedBy: {}, actualOwner: {}",
                    roomId, ownerId, room.getOwner().getId());
                throw new RuntimeException("Only owner can delete room");
            }

            String roomCode = room.getCode();
            participantRepository.deleteByRoom(room);
            roomRepository.deleteById(roomId);
            logger.info("Room deleted successfully - roomId: {}, roomCode: {}, deletedBy: {}",
                roomId, roomCode, ownerId);

            sendRoomDeletedEvent(roomId, roomCode);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Room deletion completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW OPERATION: Room deletion took {}ms", duration);
            }
        }
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomByCode(String code) {
        long startTime = System.currentTimeMillis();
        logger.debug("Starting room retrieval - code: {}", code);
        try {
            Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> {
                    logger.warn("Room not found for retrieval - code: {}", code);
                    return new RuntimeException("Room not found");
                });
            logger.debug("Room retrieved successfully - code: {}, name: {}", code);

            return new RoomResponse(
                room.getId(),
                room.getCode(),
                room.getOwner().getId(),
                room.getIsPrivate(),
                null, 
                room.getMaxParticipants(),
                room.getCreatedAt()
            );
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Room retrieval completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW OPERATION: Room retrieval took {}ms", duration);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getRoomParticipants(String roomCode) {
        long startTime = System.currentTimeMillis();
        logger.debug("Starting participants retrieval - roomCode: {}", roomCode);
        try {
            Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> {
                    logger.warn("Room not found for participants retrieval - roomCode: {}", roomCode);
                    return new RuntimeException("Room not found");
                });

            List<RoomParticipant> participants = participantRepository.findByRoom(room);
            logger.debug("Retrieved {} participants for room - roomCode: {}", participants.size(), roomCode);

            return participants.stream()
                .map(participant -> new ParticipantResponse(
                    participant.getId(),
                    participant.getUser().getUsername(),
                    participant.getUser().getId(),
                    participant.getUser().getPreferredKeyboard(),
                    participant.getJoinedAt()
                ))
                .collect(Collectors.toList());
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Participants retrieval completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW OPERATION: Participants retrieval took {}ms", duration);
            }
        }
    }

    private void sendRoomCreatedEvent(Room room) {
        try {
            Map<String, Object> eventPayload = Map.of(
                "id", room.getId().toString(),
                "code", room.getCode(),
                "ownerId", room.getOwner().getId().toString(),
                "isPrivate", room.getIsPrivate(),
                "maxParticipants", room.getMaxParticipants(),
                "createdAt", room.getCreatedAt().toString()
            );
            rabbitTemplate.convertAndSend(roomsExchange, roomCreatedRoutingKey, eventPayload, message -> {
                message.getMessageProperties().setHeader("X-Correlation-ID", CorrelationIdUtil.getCorrelationId());
                return message;
            });
            logger.debug("Room creation event sent - roomId: {}", room.getId());
        } catch (Exception e) {
            logger.warn("Failed to send room creation event - roomId: {}, error: {}", room.getId(), e.getMessage());
        }
    }

    private void sendUserJoinedEvent(Room room, LocalUser user, RoomParticipant participant) {
        try {
            Map<String, Object> eventPayload = Map.of(
                "roomId", room.getId().toString(),
                "roomCode", room.getCode(),
                "userId", user.getId().toString(),
                "participantId", participant.getId().toString(),
                "joinedAt", participant.getJoinedAt().toString()
            );
            rabbitTemplate.convertAndSend(roomsExchange, userJoinedRoutingKey, eventPayload, message -> {
                message.getMessageProperties().setHeader("X-Correlation-ID", CorrelationIdUtil.getCorrelationId());
                return message;
            });
            logger.debug("User joined event sent - roomCode: {}, userId: {}", room.getCode(), user.getId());
        } catch (Exception e) {
            logger.warn("Failed to send user joined event - roomCode: {}, userId: {}, error: {}",
                room.getCode(), user.getId(), e.getMessage());
        }
    }

    private void sendUserLeftEvent(Room room, LocalUser user) {
        try {
            Map<String, Object> eventPayload = Map.of(
                "roomId", room.getId().toString(),
                "roomCode", room.getCode(),
                "userId", user.getId().toString()
            );
            rabbitTemplate.convertAndSend(roomsExchange, userLeftRoutingKey, eventPayload, message -> {
                message.getMessageProperties().setHeader("X-Correlation-ID", CorrelationIdUtil.getCorrelationId());
                return message;
            });
            logger.debug("User left event sent - roomCode: {}, userId: {}", room.getCode(), user.getId());
        } catch (Exception e) {
            logger.warn("Failed to send user left event - roomCode: {}, userId: {}, error: {}",
                room.getCode(), user.getId(), e.getMessage());
        }
    }

    private void sendUserKickedEvent(Room room, LocalUser userToKick, UUID kickedBy) {
        try {
            Map<String, Object> eventPayload = Map.of(
                "roomId", room.getId().toString(),
                "roomCode", room.getCode(),
                "userId", userToKick.getId().toString(),
                "kickedBy", kickedBy.toString()
            );
            rabbitTemplate.convertAndSend(roomsExchange, userKickedRoutingKey, eventPayload, message -> {
                message.getMessageProperties().setHeader("X-Correlation-ID", CorrelationIdUtil.getCorrelationId());
                return message;
            });
            logger.debug("User kicked event sent - roomCode: {}, userIdKicked: {}", room.getCode(), userToKick.getId());
        } catch (Exception e) {
            logger.warn("Failed to send user kicked event - roomCode: {}, userIdKicked: {}, error: {}",
                room.getCode(), userToKick.getId(), e.getMessage());
        }
    }

    private void sendRoomDeletedEvent(UUID roomId, String roomCode) {
        try {
            Map<String, Object> eventPayload = Map.of(
                "roomId", roomId.toString(),
                "roomCode", roomCode
            );
            rabbitTemplate.convertAndSend(roomsExchange, roomDeletedRoutingKey, eventPayload, message -> {
                message.getMessageProperties().setHeader("X-Correlation-ID", CorrelationIdUtil.getCorrelationId());
                return message;
            });
            logger.debug("Room deleted event sent - roomId: {}, roomCode: {}", roomId, roomCode);
        } catch (Exception e) {
            logger.warn("Failed to send room deleted event - roomId: {}, roomCode: {}, error: {}",
                roomId, roomCode, e.getMessage());
        }
    }

    private String generateRoomCode() {
        long startTime = System.currentTimeMillis();
        try {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            logger.trace("Generated room code: {}", code);
            return code;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.trace("Room code generation completed in {}ms", duration);
        }
    }
}
