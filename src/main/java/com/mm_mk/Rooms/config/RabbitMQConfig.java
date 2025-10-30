package com.mm_mk.Rooms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String ROOMS_QUEUE_CREATED = "rooms.user.created.queue";
    public static final String ROOMS_QUEUE_UPDATED = "rooms.user.updated.queue";

    @Bean
    public MessageConverter jsonMessageConverter() {
        logger.info("Configuring Jackson2JsonMessageConverter for RabbitMQ");
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange userExchange() {
        logger.info("Creating TopicExchange: {}", USER_EXCHANGE);
        return new TopicExchange(USER_EXCHANGE, true, false);
    }

    @Bean
    public Queue userCreatedQueue() {
        logger.info("Creating Queue: {}", ROOMS_QUEUE_CREATED);
        return new Queue(ROOMS_QUEUE_CREATED, true);
    }

    @Bean
    public Queue userUpdatedQueue() {
        logger.info("Creating Queue: {}", ROOMS_QUEUE_UPDATED);
        return new Queue(ROOMS_QUEUE_UPDATED, true);
    }

    @Bean
    public Binding bindingUserCreated(Queue userCreatedQueue, TopicExchange userExchange) {
        logger.info("Binding {} to {} with routing key 'user.created'", ROOMS_QUEUE_CREATED, USER_EXCHANGE);
        return BindingBuilder.bind(userCreatedQueue).to(userExchange).with("user.created");
    }

    @Bean
    public Binding bindingUserUpdated(Queue userUpdatedQueue, TopicExchange userExchange) {
        logger.info("Binding {} to {} with routing key 'user.updated'", ROOMS_QUEUE_UPDATED, USER_EXCHANGE);
        return BindingBuilder.bind(userUpdatedQueue).to(userExchange).with("user.updated");
    }
}