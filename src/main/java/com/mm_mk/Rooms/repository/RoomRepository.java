// src/main/java/com/mm_mk/Rooms/repository/RoomRepository.java
package com.mm_mk.Rooms.repository;

import com.mm_mk.Rooms.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByCode(String code);
}