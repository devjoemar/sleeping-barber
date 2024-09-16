package com.devjoemar.sleeping_barber.service;

import com.devjoemar.sleeping_barber.event.EventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BarbershopService}.
 */
class BarbershopServiceTest {

    private static final int NUM_BARBERS = 2;
    private static final int NUM_WAITING_CHAIRS = 3;

    private BarbershopService barbershopService;
    private EventPublisher eventPublisher;

    private ExecutorService barberThreadPool;

    @BeforeEach
    void setUp() {
        // Mock EventPublisher to verify events are published correctly
        eventPublisher = mock(EventPublisher.class);

        // Initialize BarbershopService with 2 barbers and 3 waiting chairs
        barbershopService = new BarbershopService(NUM_BARBERS, NUM_WAITING_CHAIRS, eventPublisher);

        // Start barber threads before each test
        barberThreadPool = Executors.newFixedThreadPool(NUM_BARBERS);
        for (int barberId = 1; barberId <= NUM_BARBERS; barberId++) {
            int finalBarberId = barberId;
            barberThreadPool.submit(() -> {
                try {
                    barbershopService.barberEntry(finalBarberId);
                } catch (InterruptedException e) {
                    fail("Barber " + finalBarberId + " should not be interrupted.");
                }
            });
        }
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // Shutdown the barber threads after each test
        barberThreadPool.shutdownNow();
        barberThreadPool.awaitTermination(5, TimeUnit.SECONDS);
    }


    @Test
    void testCustomerReceivesHaircutSuccessfully() throws InterruptedException {
        // Arrange
        int customerId = barbershopService.generateCustomerId();

        // Act
        boolean result = barbershopService.customerEntry(customerId);

        // Assert
        assertTrue(result, "Customer should receive a haircut.");
        verify(eventPublisher).publishEvent(eq("customerWaiting"), eq(customerId));
        verify(eventPublisher).publishEvent(eq("customerSeated"), eq(customerId), anyInt());
        verify(eventPublisher).publishEvent(eq("customerFinished"), eq(customerId), anyInt());
    }

    @Test
    void testBarberServesCustomerSuccessfully() throws InterruptedException {
        // Arrange
        int barberId = 1;
        int customerId = barbershopService.generateCustomerId();
        barbershopService.customerEntry(customerId);

        // Act: Barber starts work
        Runnable barberTask = () -> {
            try {
                barbershopService.barberEntry(barberId);
            } catch (InterruptedException e) {
                fail("Barber should not be interrupted.");
            }
        };
        ExecutorService barberThread = Executors.newSingleThreadExecutor();
        barberThread.submit(barberTask);

        // Allow some time for the barber to serve the customer
        barberThread.shutdown();
        barberThread.awaitTermination(5, TimeUnit.SECONDS);

        // Assert: Check that the barber serves the customer
        verify(eventPublisher).publishEvent(eq("barberStarted"), eq(barberId), anyInt(), eq(customerId));
        verify(eventPublisher).publishEvent(eq("barberFinished"), eq(barberId), anyInt(), eq(customerId));
    }

    @Test
    void testMultipleCustomersHandledConcurrently() throws InterruptedException {
        // Arrange
        int customer1Id = barbershopService.generateCustomerId();
        int customer2Id = barbershopService.generateCustomerId();

        // Act: Simulate two customers entering concurrently
        ExecutorService customerThread = Executors.newFixedThreadPool(2);
        customerThread.submit(() -> {
            try {
                barbershopService.customerEntry(customer1Id);
            } catch (InterruptedException e) {
                fail("Customer 1 should not be interrupted.");
            }
        });
        customerThread.submit(() -> {
            try {
                barbershopService.customerEntry(customer2Id);
            } catch (InterruptedException e) {
                fail("Customer 2 should not be interrupted.");
            }
        });

        customerThread.shutdown();
        customerThread.awaitTermination(5, TimeUnit.SECONDS);

        // Assert: Verify that both customers were handled
        verify(eventPublisher, atLeast(1)).publishEvent(eq("customerWaiting"), anyInt());
        verify(eventPublisher, atLeast(1)).publishEvent(eq("customerSeated"), anyInt(), anyInt());
        verify(eventPublisher, atLeast(1)).publishEvent(eq("customerFinished"), anyInt(), anyInt());
    }

    @Test
    void testBarberWaitsForCustomersWhenNoCustomersAvailable() throws InterruptedException {
        // Arrange
        int barberId = 1;

        // Act: Start barber without any customers
        Runnable barberTask = () -> {
            try {
                barbershopService.barberEntry(barberId);
            } catch (InterruptedException e) {
                fail("Barber should not be interrupted.");
            }
        };
        ExecutorService barberThread = Executors.newSingleThreadExecutor();
        barberThread.submit(barberTask);

        // Allow the barber to start and wait for a customer
        barberThread.shutdown();
        assertFalse(barberThread.awaitTermination(2, TimeUnit.SECONDS), "Barber should wait for customers indefinitely.");
    }

}
