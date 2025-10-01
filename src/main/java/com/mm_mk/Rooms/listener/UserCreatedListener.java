// src/main/java/com/mm_mk/Rooms/listener/UserCreatedListener.java
package com.mm_mk.Rooms.listener;

import com.mm_mk.Rooms.model.LocalUser;
import com.mm_mk.Rooms.repository.LocalUserRepository;
import com.mm_mk.Rooms.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserCreatedListener {

    private final LocalUserRepository localUserRepository;

    @RabbitListener(queues = "rooms.user.queue")
    @Transactional
    public void handleUserCreated(UserCreatedEvent event) {
        // Only insert if not already exists
        if (!localUserRepository.existsById(event.id())) {
            LocalUser localUser = LocalUser.builder()
                    .id(event.id())
                    .username(event.username())
                    .build();

            localUserRepository.save(localUser);
        }
    }
}
