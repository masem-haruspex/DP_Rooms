package com.mm_mk.Rooms.repository;

import com.mm_mk.Rooms.model.Room;
import com.mm_mk.Rooms.model.RoomParticipant;
import com.mm_mk.Rooms.model.LocalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, UUID> {

    /**
     * Find all participants by the room.
     */
    List<RoomParticipant> findByRoom(Room room);

    /**
     * Check if a user is already a participant in a given room.
     */
    boolean existsByRoomAndUser(Room room, LocalUser user);

    /**
     * Delete all participants for a given room.
     */
    void deleteByRoom(Room room);

    /**
     * Remove a specific user from a specific room.
     */
    void deleteByRoomAndUser(Room room, LocalUser user);
}
