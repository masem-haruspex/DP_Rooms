package com.mm_mk.Rooms.service;

import com.mm_mk.Rooms.model.LocalUser;
import com.mm_mk.Rooms.model.Room;
import com.mm_mk.Rooms.model.RoomParticipant;
import com.mm_mk.Rooms.repository.LocalUserRepository;
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
	private final LocalUserRepository localUserRepository;
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
			LocalUserRepository localUserRepository,
			RabbitTemplate rabbitTemplate) {
		this.roomRepository = roomRepository;
		this.participantRepository = participantRepository;
		this.localUserRepository = localUserRepository;
		this.rabbitTemplate = rabbitTemplate;
	}

	@Transactional
	public RoomResponse createRoom(UUID ownerId, String name, Boolean isPrivate, String password, Integer maxParticipants) {
		LocalUser owner = localUserRepository.findById(ownerId)
			.orElseThrow(() -> new RuntimeException("Owner not found in local_users"));

		UUID roomId = UUID.randomUUID();
		String code = generateRoomCode();

		Room room = Room.builder()
			.id(roomId)
			.code(code)
			.name(name)
			.owner(owner)
			.isPrivate(isPrivate != null ? isPrivate : false)
			.password(password)
			.maxParticipants(maxParticipants != null ? maxParticipants : 2)
			.createdAt(LocalDateTime.now())
			.build();

		room = roomRepository.save(room);

		rabbitTemplate.convertAndSend(roomsExchange, roomCreatedRoutingKey, room);

		return new RoomResponse(
				room.getId(),
				room.getCode(),
				room.getName(),
				room.getOwner().getId(),
				room.getIsPrivate(),
				room.getPassword(),
				room.getMaxParticipants(),
				room.getCreatedAt()
				);
	}

	@Transactional
	public JoinRoomResponse joinRoom(String roomCode, UUID userId, String password) {
		try {
			Room room = roomRepository.findByCode(roomCode)
				.orElseThrow(() -> new RuntimeException("Room not found"));

			LocalUser user = localUserRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found in local_users"));

			if (room.getIsPrivate() && room.getPassword() != null && !room.getPassword().isEmpty()) {
				if (password == null || !password.equals(room.getPassword())) {
					throw new RuntimeException("Invalid room password");
				}
			}

			if (participantRepository.existsByRoomAndUser(room, user)) {
				throw new RuntimeException("User already in room");
			}

			long participantCount = participantRepository.findByRoom(room).size();
			if (participantCount >= room.getMaxParticipants()) {
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

			//rabbitTemplate.convertAndSend(roomsExchange, userJoinedRoutingKey, participant);
			rabbitTemplate.convertAndSend(roomsExchange, userJoinedRoutingKey,
					Map.of(
						"roomId", room.getId().toString(),
						"userId", user.getId().toString(),
						"participantId", participant.getId().toString(),
						"joinedAt", participant.getJoinedAt().toString()
						)
					);

			return new JoinRoomResponse(
					participant.getRoom().getId(),
					participant.getUser().getId(),
					participant.getJoinedAt()
					);
		} catch (RuntimeException e) {
			System.err.println("Join room error: " + e.getMessage());
			throw e;
		}
	}

	public RoomResponse getRoomByCode(String code) {
		Room room = roomRepository.findByCode(code)
			.orElseThrow(() -> new RuntimeException("Room not found"));

		return new RoomResponse(
				room.getId(),
				room.getCode(),
				room.getName(),
				room.getOwner().getId(),
				room.getIsPrivate(),
				room.getPassword(),
				room.getMaxParticipants(),
				room.getCreatedAt()
				);
	}

	@Transactional
	public void deleteRoom(UUID roomId, UUID ownerId) {
		Room room = roomRepository.findById(roomId)
			.orElseThrow(() -> new RuntimeException("Room not found"));

		if (!room.getOwner().getId().equals(ownerId)) {
			throw new RuntimeException("Only owner can delete room");
		}

		participantRepository.deleteByRoom(room);

		roomRepository.deleteById(roomId);

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

		LocalUser user = localUserRepository.findById(userId)
			.orElseThrow(() -> new RuntimeException("User not found in local_users"));

		if (!participantRepository.existsByRoomAndUser(room, user)) {
			throw new RuntimeException("User not in room");
		}

		participantRepository.deleteByRoomAndUser(room, user);

		rabbitTemplate.convertAndSend(
				roomsExchange,
				userLeftRoutingKey,
				Map.of("roomId", room.getId(), "userId", user.getId())
				);
	}

	@Transactional
	public void kickUser(String roomCode, UUID ownerId, UUID userIdToKick) {
		Room room = roomRepository.findByCode(roomCode)
			.orElseThrow(() -> new RuntimeException("Room not found"));

		if (!room.getOwner().getId().equals(ownerId)) {
			throw new RuntimeException("Only owner can kick");
		}

		if (userIdToKick.equals(ownerId)) {
			throw new RuntimeException("Owner cannot kick themselves");
		}

		LocalUser userToKick = localUserRepository.findById(userIdToKick)
			.orElseThrow(() -> new RuntimeException("User to kick not found"));

		participantRepository.deleteByRoomAndUser(room, userToKick);

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

		if (!room.getOwner().getId().equals(ownerId)) {
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

	private String generateRoomCode() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
	}
}
