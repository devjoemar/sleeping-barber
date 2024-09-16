package com.devjoemar.sleeping_barber.controller;

import com.devjoemar.sleeping_barber.service.BarbershopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BarbershopController {

    private static final Logger LOG = LoggerFactory.getLogger(BarbershopController.class);

    private final BarbershopService barbershopService;

    public BarbershopController(BarbershopService barbershopService) {
        this.barbershopService = barbershopService;
    }

    @GetMapping("/api/barber-chairs")
    public int getNumberOfBarberChairs() {
        return barbershopService.getNumberOfBarbers();
    }

    @PostMapping("/api/add-customer")
    public ResponseEntity<String> addCustomer() {
        int customerId = barbershopService.generateCustomerId();
        Thread.startVirtualThread(() -> {
            Thread.currentThread().setName("Customer-" + customerId);
            try {
                boolean entered = barbershopService.customerEntry(customerId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        return ResponseEntity.ok("Customer added");
    }
}
