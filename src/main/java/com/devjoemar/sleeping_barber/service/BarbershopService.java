package com.devjoemar.sleeping_barber.service;

import com.devjoemar.sleeping_barber.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@code BarbershopService} class simulates the Sleeping Barber Problem using Java concurrency primitives.
 *
 * <p><b>Problem Statement:</b></p>
 * The Sleeping Barber Problem involves a barbershop where barbers can sleep when no customers are present.
 * There are a limited number of barber chairs and waiting room chairs. Customers arriving at the shop will:
 * 1. Get a haircut if a barber is available.
 * 2. Sit in the waiting room if there are empty waiting chairs.
 * 3. Leave if all waiting chairs are occupied.
 * Barbers sleep when there are no customers, and customers wake barbers when they need service.
 *
 * <p><b>Solution Overview:</b></p>
 * This class uses semaphores to manage synchronization between customers and barbers. Semaphores control access
 * to the waiting room, barber chairs, and barbers. The system ensures that:
 * 1. Customers leave if no waiting room chairs are available.
 * 2. Barbers only serve one customer at a time.
 * 3. The shop remains functional even with concurrent customer and barber actions.
 *
 * <p>If synchronization is not handled correctly, the system can face issues like deadlocks or race conditions,
 * causing customers to wait indefinitely or barbers to cut no one's hair.</p>
 */
public class BarbershopService {

    private static final Logger LOG = LoggerFactory.getLogger(BarbershopService.class);

    /** The number of barbers in the barbershop. */
    private final int numberOfBarbers;

    /** Mutex semaphore to control access to the waiting room. */
    private final Semaphore waitingRoomMutex = new Semaphore(1);

    /** Mutex semaphore to control access to the barber room. */
    private final Semaphore barberRoomMutex = new Semaphore(1);

    /** Semaphore to represent the number of available barber chairs. */
    private final Semaphore barberChairSemaphore;

    /** Semaphore used by customers to wake up barbers. */
    private final Semaphore sleepyBarbers = new Semaphore(0);

    /** Array of semaphores representing individual barber chairs. */
    private final Semaphore[] barberChairs;

    /**
     * Tracks the state of each barber chair:
     * 0: empty,
     * 1: waiting for haircut,
     * 2: haircut in progress
     */
    private final int[] barberChairStates;

    /** Tracks which customer is in each barber chair. */
    private final int[] barberChairCustomerIds;

    /** Publishes events to external systems or UI components (e.g., user interfaces or dashboards). */
    private final EventPublisher eventPublisher;

    /** Generates unique customer IDs for each new customer. */
    private final AtomicInteger customerIdGenerator = new AtomicInteger(1);

    /** Tracks the number of available waiting chairs in the waiting room. */
    private int availableWaitingChairs;

    /**
     * Initializes a new instance of the {@code BarbershopService} class.
     *
     * @param numberOfBarbers       The number of barbers working in the shop.
     * @param numberOfWaitingChairs The number of waiting chairs in the waiting room.
     * @param eventPublisher        The event publisher for handling external events.
     */
    public BarbershopService(int numberOfBarbers, int numberOfWaitingChairs, EventPublisher eventPublisher) {
        this.numberOfBarbers = numberOfBarbers;
        this.availableWaitingChairs = numberOfWaitingChairs;
        this.eventPublisher = eventPublisher;

        // Semaphore to limit the number of customers in the barber chairs
        this.barberChairSemaphore = new Semaphore(numberOfBarbers);
        this.barberChairs = new Semaphore[numberOfBarbers];
        this.barberChairStates = new int[numberOfBarbers];
        this.barberChairCustomerIds = new int[numberOfBarbers];

        // Initialize each barber chair as empty
        for (int i = 0; i < numberOfBarbers; i++) {
            barberChairs[i] = new Semaphore(0);  // Semaphore for each barber chair, initialized to 0
            barberChairStates[i] = 0;            // 0: Chair is initially empty
            barberChairCustomerIds[i] = -1;      // No customer in the chair (-1)
        }

        LOG.info("Barbershop initialized with {} barbers and {} waiting chairs.", numberOfBarbers, numberOfWaitingChairs);
    }

    /**
     * Gets the number of barbers in the barbershop.
     *
     * @return The number of barbers.
     */
    public int getNumberOfBarbers() {
        return this.numberOfBarbers;
    }

    /**
     * Generates a unique customer ID for a new customer.
     *
     * @return A unique customer ID.
     */
    public int generateCustomerId() {
        return customerIdGenerator.getAndIncrement();
    }

    /**
     * Represents a customer entering the barbershop.
     *
     * <p>The customer will:
     * 1. Enter the waiting room if there are available waiting chairs.
     * 2. Wait for a barber chair if all barbers are busy.
     * 3. Leave the shop if no waiting chairs are available.</p>
     *
     * @param customerId The unique ID of the customer entering the shop.
     * @return {@code true} if the customer receives a haircut, {@code false} if the customer leaves.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public boolean customerEntry(int customerId) throws InterruptedException {
        LOG.debug("Customer {} is trying to enter the barbershop.", customerId);

        // Attempt to enter the waiting room
        waitingRoomMutex.acquire();
        if (availableWaitingChairs == 0) {
            LOG.debug("No waiting chairs available. Customer {} is leaving.", customerId);
            waitingRoomMutex.release();
            eventPublisher.publishEvent("customerLeft", customerId); // Publish an event if customer leaves
            return false; // Customer leaves due to no available waiting chairs
        }

        availableWaitingChairs--; // Reduce the number of available waiting chairs
        LOG.debug("Customer {} is waiting. Available waiting chairs: {}", customerId, availableWaitingChairs);
        eventPublisher.publishEvent("customerWaiting", customerId); // Publish an event indicating customer is waiting
        waitingRoomMutex.release();

        // Wait for a barber chair to become available
        LOG.debug("Customer {} is waiting for a barber chair.", customerId);
        barberChairSemaphore.acquire();  // Wait until a barber chair is free

        // Leave the waiting room
        waitingRoomMutex.acquire();
        availableWaitingChairs++; // Increase the number of available waiting chairs as customer moves to a barber chair
        LOG.debug("Customer {} is leaving the waiting room. Available waiting chairs: {}", customerId, availableWaitingChairs);
        eventPublisher.publishEvent("customerLeftWaitingRoom", customerId); // Publish event for leaving waiting room
        waitingRoomMutex.release();

        // Find an available barber chair
        barberRoomMutex.acquire();
        int myChair = -1;
        for (int i = 0; i < numberOfBarbers; i++) {
            if (barberChairStates[i] == 0) { // Find an empty barber chair
                myChair = i;
                barberChairStates[i] = 1; // Mark chair as waiting for haircut
                barberChairCustomerIds[i] = customerId; // Assign customer to chair
                LOG.debug("Customer {} is seated at barber chair {}.", customerId, myChair);
                break;
            }
        }
        eventPublisher.publishEvent("customerSeated", customerId, myChair); // Publish event for customer being seated
        barberRoomMutex.release();

        // Notify barbers that a customer is ready
        sleepyBarbers.release();
        LOG.debug("Customer {} notified barbers.", customerId);

        // Wait for haircut to finish
        LOG.debug("Customer {} is waiting for haircut to finish at chair {}.", customerId, myChair);
        barberChairs[myChair].acquire(); // Customer waits while barber cuts hair

        eventPublisher.publishEvent("customerFinished", customerId, myChair); // Publish event for finished haircut
        LOG.debug("Customer {} finished haircut at chair {}.", customerId, myChair);

        // Release barber chair
        barberChairSemaphore.release();
        LOG.debug("Customer {} is leaving the barbershop.", customerId);

        return true; // Customer successfully received a haircut
    }

    /**
     * Represents a barber starting their workday and serving customers.
     *
     * <p>The barber will wait for customers to notify them, serve them by cutting their hair,
     * and then wait for the next customer.</p>
     *
     * @param barberId The unique ID of the barber.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public void barberEntry(int barberId) throws InterruptedException {
        LOG.info("Barber {} started working.", barberId);
        while (true) {
            // Wait for a customer
            LOG.debug("Barber {} is waiting for a customer.", barberId);
            sleepyBarbers.acquire(); // Barber sleeps until a customer wakes them up
            LOG.debug("Barber {} is ready to serve a customer.", barberId);

            // Find the next customer to serve
            barberRoomMutex.acquire();
            int myChair = -1;
            int customerId = -1;
            for (int i = 0; i < numberOfBarbers; i++) {
                if (barberChairStates[i] == 1) { // Find a customer waiting for a haircut
                    myChair = i;
                    customerId = barberChairCustomerIds[i];
                    barberChairStates[i] = 2; // Mark haircut as in progress
                    LOG.debug("Barber {} is starting haircut for customer {} at chair {}.", barberId, customerId, myChair);
                    break;
                }
            }
            eventPublisher.publishEvent("barberStarted", barberId, myChair, customerId); // Publish event for haircut start
            barberRoomMutex.release();

            // Simulate haircut
            cutHair(barberId, myChair);

            // After haircut is done
            barberRoomMutex.acquire();
            barberChairStates[myChair] = 0; // Mark chair as empty
            barberChairCustomerIds[myChair] = -1; // Reset customer ID for the chair
            barberChairs[myChair].release(); // Notify customer that the haircut is done
            LOG.debug("Barber {} finished haircut for customer {} at chair {}.", barberId, customerId, myChair);
            eventPublisher.publishEvent("barberFinished", barberId, myChair, customerId); // Publish event for finished haircut
            barberRoomMutex.release();
        }
    }

    /**
     * Simulates the haircut process by causing the thread to sleep for a specified duration.
     *
     * @param barberId The ID of the barber performing the haircut.
     * @param chairId  The ID of the barber chair being used.
     * @throws InterruptedException If the thread is interrupted while sleeping.
     */
    private void cutHair(int barberId, int chairId) throws InterruptedException {
        LOG.debug("Barber {} is cutting hair at chair {}.", barberId, chairId);
        // Simulate haircut duration (3 seconds)
        Thread.sleep(3000);
        LOG.debug("Barber {} finished cutting hair at chair {}.", barberId, chairId);
    }
}
