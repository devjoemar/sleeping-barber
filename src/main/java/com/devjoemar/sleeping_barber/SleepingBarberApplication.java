package com.devjoemar.sleeping_barber;

import com.devjoemar.sleeping_barber.service.BarbershopService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SleepingBarberApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(SleepingBarberApplication.class, args);

        // Get the Barbershop bean from the context
        BarbershopService barbershopService = context.getBean(BarbershopService.class);

        // Start barber threads
        for (int i = 0; i < barbershopService.getNumberOfBarbers(); i++) {
            int barberId = i;
            Thread.startVirtualThread(() -> {
                Thread.currentThread().setName("Barber-" + barberId);
                try {
                    barbershopService.barberEntry(barberId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
}