package com.mm_mk.Rooms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rooms", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 12)
    @Pattern(regexp = "^[A-Z0-9]{12}$", message = "Code must be 12 uppercase alphanumeric characters")
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rooms_owner"))
    private LocalUser owner;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate = false;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "max_participants", nullable = false)
    @Min(value = 2, message = "Minimum 2 participants")
    @Max(value = 10, message = "Maximum 10 participants")
    private Integer maxParticipants = 2;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
