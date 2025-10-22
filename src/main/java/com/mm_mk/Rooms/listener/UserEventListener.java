package com.mm_mk.Rooms.listener;

import com.mm_mk.Rooms.event.UserUpdatedEvent;
import com.mm_mk.Rooms.model.LocalUser;
import com.mm_mk.Rooms.repository.LocalUserRepository;
import com.mm_mk.Rooms.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final LocalUserRepository localUserRepository;

    @RabbitListener(queues = "rooms.user.created.queue")
    @Transactional
    public void handleUserEvent(UserCreatedEvent event) {
        // Only insert if not already exists
        if (!localUserRepository.existsById(event.id())) {
            LocalUser localUser = LocalUser.builder()
                    .id(event.id())
                    .username(event.username())
                    .build();

            localUserRepository.save(localUser);
        }
    }

    @RabbitListener(queues = "rooms.user.updated.queue")
    @Transactional
    public void handleUserUpdated(UserUpdatedEvent event) {
        localUserRepository.findById(event.id()).ifPresent(existingUser -> {
            existingUser.setUsername(event.username());
            existingUser.setPreferredKeyboard(event.preferredKeyboard());
            localUserRepository.save(existingUser);
        });
    }
}
