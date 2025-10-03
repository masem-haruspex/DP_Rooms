package com.mm_mk.Rooms.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String USER_EXCHANGE = "user.exchange";
    public static final String ROOMS_QUEUE = "rooms.user.queue";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public FanoutExchange userExchange() {
        return new FanoutExchange(USER_EXCHANGE);
    }

    @Bean
    public Queue roomsQueue() {
        return new Queue(ROOMS_QUEUE, true);
    }

    @Bean
    public Binding binding(Queue roomsQueue, FanoutExchange userExchange) {
        return BindingBuilder.bind(roomsQueue).to(userExchange);
    }
}