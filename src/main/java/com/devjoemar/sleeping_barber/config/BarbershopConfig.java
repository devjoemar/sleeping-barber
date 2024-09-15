package com.devjoemar.sleeping_barber.config;

import com.devjoemar.sleeping_barber.event.EventPublisher;
import com.devjoemar.sleeping_barber.service.BarbershopService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BarbershopConfig {

    @Bean
    public BarbershopService barbershop(
            @Value("${number.of.barbers}") int numberOfBarbers,
            @Value("${number.of.waiting.chairs}") int numberOfWaitingChairs,
            EventPublisher eventPublisher) {
        return new BarbershopService(numberOfBarbers, numberOfWaitingChairs, eventPublisher);
    }
}
