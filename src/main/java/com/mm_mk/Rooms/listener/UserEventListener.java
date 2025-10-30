package com.mm_mk.Rooms.listener;

import com.mm_mk.Rooms.event.UserUpdatedEvent;
import com.mm_mk.Rooms.model.LocalUser;
import com.mm_mk.Rooms.repository.LocalUserRepository;
import com.mm_mk.Rooms.event.UserCreatedEvent;
import com.mm_mk.Rooms.util.CorrelationIdUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private static final Logger logger = LoggerFactory.getLogger(UserEventListener.class);
    private static final long SLOW_OPERATION_THRESHOLD_MS = 500;

    private final LocalUserRepository localUserRepository;

    @RabbitListener(queues = "rooms.user.created.queue")
    @Transactional
    public void handleUserEvent(UserCreatedEvent event,
                                @Header(name = "X-Correlation-ID", required = false) String correlationId,
                                @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {

        String eventCorrelationId = correlationId != null ? correlationId :
                "EVENT-" + CorrelationIdUtil.generateCorrelationId();
        CorrelationIdUtil.setCorrelationId(eventCorrelationId);

        long startTime = System.currentTimeMillis();
        logger.info("Processing UserCreatedEvent - userId: {}, username: {}, routingKey: {}",
                event.id(), event.username(), routingKey);

        try {
            if (!localUserRepository.existsById(event.id())) {
                LocalUser localUser = LocalUser.builder()
                        .id(event.id())
                        .username(event.username())
                        .build();

                localUserRepository.save(localUser);
                logger.info("User created successfully - userId: {}, username: {}", event.id(), event.username());
            } else {
                logger.debug("User already exists, skipping creation - userId: {}", event.id());
            }
        } catch (Exception e) {
            logger.error("Failed to process UserCreatedEvent - userId: {}, username: {}, error: {}",
                    event.id(), event.username(), e.getMessage(), e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("UserCreatedEvent processing completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW OPERATION: UserCreatedEvent processing took {}ms", duration);
            }
            CorrelationIdUtil.clear();
        }
    }

    @RabbitListener(queues = "rooms.user.updated.queue")
    @Transactional
    public void handleUserUpdated(UserUpdatedEvent event,
                                  @Header(name = "X-Correlation-ID", required = false) String correlationId,
                                  @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {

        String eventCorrelationId = correlationId != null ? correlationId :
                "EVENT-" + CorrelationIdUtil.generateCorrelationId();
        CorrelationIdUtil.setCorrelationId(eventCorrelationId);

        long startTime = System.currentTimeMillis();
        logger.info("Processing UserUpdatedEvent - userId: {}, username: {}, preferredKeyboard: {}, routingKey: {}",
                event.id(), event.username(), event.preferredKeyboard(), routingKey);

        try {
            localUserRepository.findById(event.id()).ifPresentOrElse(
                    existingUser -> {
                        existingUser.setUsername(event.username());
                        existingUser.setPreferredKeyboard(event.preferredKeyboard());
                        localUserRepository.save(existingUser);
                        logger.info("User updated successfully - userId: {}, newUsername: {}, newKeyboard: {}",
                                event.id(), event.username(), event.preferredKeyboard());
                    },
                    () -> logger.warn("User not found for update - userId: {}", event.id())
            );
        } catch (Exception e) {
            logger.error("Failed to process UserUpdatedEvent - userId: {}, error: {}", event.id(), e.getMessage(), e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("UserUpdatedEvent processing completed in {}ms", duration);
            if (duration > SLOW_OPERATION_THRESHOLD_MS) {
                logger.warn("SLOW OPERATION: UserUpdatedEvent processing took {}ms", duration);
            }
            CorrelationIdUtil.clear();
        }
    }
}