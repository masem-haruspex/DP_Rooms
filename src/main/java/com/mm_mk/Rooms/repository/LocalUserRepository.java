package com.mm_mk.Rooms.repository;

import com.mm_mk.Rooms.model.LocalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocalUserRepository extends JpaRepository<LocalUser, UUID> {

    /**
     * Find a local user by their username.
     */
    Optional<LocalUser> findByUsername(String username);

    /**
     * Check if a user exists by username.
     */
    boolean existsByUsername(String username);
}
