package org.example.taxi.trip.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String Q_PASSENGER_EXISTS = "user.passenger.exists";
    public static final String Q_DRIVER_ASSIGN = "user.driver.assign";
    public static final String Q_DRIVER_STATUS_UPDATE = "user.driver.status.update";
    public static final String Q_NOTIFICATION_CREATE = "notification.create.task";

    @Bean
    public Queue passengerExistsQueue() { return new Queue(Q_PASSENGER_EXISTS); }

    @Bean
    public Queue driverAssignQueue() { return new Queue(Q_DRIVER_ASSIGN); }

    @Bean
    public Queue driverStatusUpdateQueue() { return new Queue(Q_DRIVER_STATUS_UPDATE); }

    @Bean
    public Queue notificationCreateQueue() { return new Queue(Q_NOTIFICATION_CREATE); }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
