// src/main/java/com/mm_mk/Rooms/config/RabbitMQConfig.java
package com.mm_mk.Rooms.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // INFO: Spring boot has a default rabbitTemplate bean
    // INFO: You can declare exchanges/queues here if needed, but often done in other services
}