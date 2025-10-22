package com.mm_mk.Rooms.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String USER_EXCHANGE = "user.exchange";
    public static final String ROOMS_QUEUE_CREATED = "rooms.user.created.queue";
    public static final String ROOMS_QUEUE_UPDATED = "rooms.user.updated.queue";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE, true, false);
    }

    @Bean
    public Queue userCreatedQueue() {
        return new Queue(ROOMS_QUEUE_CREATED, true);
    }

    @Bean
    public Queue userUpdatedQueue() {
        return new Queue(ROOMS_QUEUE_UPDATED, true);
    }

    @Bean
    public Binding bindingUserCreated(Queue userCreatedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userCreatedQueue).to(userExchange).with("user.created");
    }

    @Bean
    public Binding bindingUserUpdated(Queue userUpdatedQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userUpdatedQueue).to(userExchange).with("user.updated");
    }


}