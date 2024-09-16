package com.devjoemar.sleeping_barber.service;

import com.devjoemar.sleeping_barber.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@code BarbershopService} class simulates the Sleeping Barber Problem using Java concurrency primitives.
 * It manages customers entering the shop, waiting for barbers, and receiving haircuts, while barbers are managed concurrently.
 *
 * <p><b>Problem Statement:</b></p>
 * In the Sleeping Barber Problem, a barbershop has a limited number of barbers and waiting chairs. Customers may:
 * 1. Enter the shop if there is space in the waiting room or get a haircut immediately if a barber is available.
 * 2. Wait in the waiting room if no barbers are free but chairs are available.
 * 3. Leave if no chairs are available.
 *
 * <p><b>Solution Approach:</b></p>
 * We use Java concurrency primitives such as semaphores to manage access to shared resources:
 * - The barber chairs and waiting room must be carefully controlled to avoid race conditions and deadlocks.
 * - Customers interact with barbers through semaphore-based synchronization, ensuring a well-coordinated system where barbers and customers interact efficiently.
 */
public class BarbershopService {

    private static final Logger LOG = LoggerFactory.getLogger(BarbershopService.class);

    /** The number of barbers working in the shop. */
    private final int numberOfBarbers;

    /** Semaphore to control access to the waiting room to prevent race conditions. */
    private final Semaphore waitingRoomMutex = new Semaphore(1);

    /** Semaphore to control access to the barber room. */
    private final Semaphore barberRoomMutex = new Semaphore(1);

    /** Semaphore to represent the number of available barber chairs. */
    private final Semaphore barberChairSemaphore;

    /** Semaphore used by customers to wake up barbers when they are ready for a haircut. */
    private final Semaphore sleepyBarbers = new Semaphore(0);

    /** Array of semaphores representing each barber chair. */
    private final Semaphore[] barberChairs;

    /**
     * Tracks the state of each barber chair:
     * 0: empty, 1: waiting for haircut, 2: haircut in progress.
     */
    private final int[] barberChairStates;

    /** Tracks which customer is in each barber chair. */
    private final int[] barberChairCustomerIds;

    /** Publishes real-time events to external systems (e.g., UI or monitoring dashboard) using WebSockets. */
    private final EventPublisher eventPublisher;

    /** Generates unique IDs for each customer entering the shop. */
    private final AtomicInteger customerIdGenerator = new AtomicInteger(1);

    /** Tracks the number of available waiting room chairs. */
    private int availableWaitingChairs;

    /**
     * Initializes a new instance of the {@code BarbershopService} class.
     *
     * @param numberOfBarbers       The number of barbers in the shop.
     * @param numberOfWaitingChairs The number of chairs available in the waiting room.
     * @param eventPublisher        The event publisher for notifying external systems of events.
     */
    public BarbershopService(int numberOfBarbers, int numberOfWaitingChairs, EventPublisher eventPublisher) {
        this.numberOfBarbers = numberOfBarbers;
        this.availableWaitingChairs = numberOfWaitingChairs;
        this.eventPublisher = eventPublisher;

        // Initialize barber chair states and customer tracking
        this.barberChairStates = new int[numberOfBarbers];
        this.barberChairCustomerIds = new int[numberOfBarbers];

        // Semaphore to limit the number of customers in the barber chairs
        this.barberChairSemaphore = new Semaphore(numberOfBarbers);
        this.barberChairs = initializeBarberChairs(numberOfBarbers);

        LOG.info("Barbershop initialized with {} barbers and {} waiting chairs.", numberOfBarbers, numberOfWaitingChairs);
    }

    /**
     * Initializes the semaphores for each barber chair and sets the chair states to empty.
     *
     * <p><b>Rationale:</b></p>
     * We initialize each barber chair semaphore to 0, indicating that no customers are seated. The states and customer IDs
     * are also initialized to reflect empty chairs. This prepares the system for managing customer-barber interactions safely.
     *
     * @param numberOfBarbers The number of barbers.
     * @return An array of semaphores representing barber chairs.
     */
    private Semaphore[] initializeBarberChairs(int numberOfBarbers) {
        Semaphore[] chairs = new Semaphore[numberOfBarbers];
        for (int i = 0; i < numberOfBarbers; i++) {
            chairs[i] = new Semaphore(0);  // Each chair starts empty
            barberChairStates[i] = 0;       // Chair is empty
            barberChairCustomerIds[i] = -1; // No customer in the chair
        }
        return chairs;
    }

    /**
     * Generates a unique customer ID.
     *
     * <p><b>Rationale:</b></p>
     * Each customer needs a unique identifier to track them throughout the barbershop process.
     * This ID is used in events and for tracking their interactions with barbers.
     *
     * @return A unique customer ID.
     */
    public int generateCustomerId() {
        return customerIdGenerator.getAndIncrement();
    }

    public int getNumberOfBarbers() {
        return this.numberOfBarbers;
    }

    /**
     * A customer enters the barbershop.
     *
     * <p>If the waiting room is full, the customer leaves. If space is available, the customer waits for a barber to be free
     * or gets a haircut immediately if a barber is ready.</p>
     *
     * <p><b>Rationale:</b></p>
     * The logic ensures that the waiting room capacity is respected. If no space is available, the customer leaves
     * without interacting with the system further. Otherwise, they wait for an available barber.
     *
     * @param customerId The unique ID of the customer entering the shop.
     * @return {@code true} if the customer received a haircut; {@code false} if they left due to lack of space.
     * @throws InterruptedException If the thread is interrupted while waiting for a barber.
     */
    public boolean customerEntry(int customerId) throws InterruptedException {
        LOG.debug("Customer {} is attempting to enter the barbershop.", customerId);

        // Attempt to enter the waiting room
        if (!enterWaitingRoom(customerId)) {
            return false;
        }

        // Transition from waiting room to barber chair
        moveFromWaitingRoomToBarberChair(customerId);
        notifyBarbers(customerId);

        // Wait for the haircut to finish
        return waitForHaircutCompletion(customerId);
    }

    /**
     * Customer attempts to enter the waiting room. If no chairs are available, the customer leaves.
     *
     * @param customerId The unique ID of the customer.
     * @return {@code true} if the customer successfully entered the waiting room; {@code false} if the customer left.
     * @throws InterruptedException If the thread is interrupted while acquiring the mutex.
     */
    private boolean enterWaitingRoom(int customerId) throws InterruptedException {
        waitingRoomMutex.acquire();
        if (availableWaitingChairs == 0) {
            LOG.debug("No waiting chairs available. Customer {} is leaving.", customerId);
            waitingRoomMutex.release();
            eventPublisher.publishEvent("customerLeft", customerId);
            return false;
        }

        availableWaitingChairs--; // One less waiting chair available
        LOG.debug("Customer {} is waiting. Available waiting chairs: {}", customerId, availableWaitingChairs);
        eventPublisher.publishEvent("customerWaiting", customerId);
        waitingRoomMutex.release();
        return true;
    }

    /**
     * Moves the customer from the waiting room to a barber chair once available.
     *
     * @param customerId The unique ID of the customer.
     * @throws InterruptedException If the thread is interrupted while waiting for a barber chair.
     */
    private void moveFromWaitingRoomToBarberChair(int customerId) throws InterruptedException {
        barberChairSemaphore.acquire(); // Wait until a barber chair is available

        // Customer leaves the waiting room
        waitingRoomMutex.acquire();
        availableWaitingChairs++;
        LOG.debug("Customer {} is leaving the waiting room. Available waiting chairs: {}", customerId, availableWaitingChairs);
        eventPublisher.publishEvent("customerLeftWaitingRoom", customerId);
        waitingRoomMutex.release();

        // Find and assign a barber chair to the customer
        assignCustomerToBarberChair(customerId);
    }

    /**
     * Assigns the customer to an available barber chair.
     *
     * @param customerId The unique ID of the customer.
     * @throws InterruptedException If the thread is interrupted while assigning the barber chair.
     */
    private void assignCustomerToBarberChair(int customerId) throws InterruptedException {
        barberRoomMutex.acquire();
        for (int i = 0; i < numberOfBarbers; i++) {
            if (barberChairStates[i] == 0) { // Look for an empty chair
                barberChairStates[i] = 1; // Mark chair as waiting for haircut
                barberChairCustomerIds[i] = customerId;
                LOG.debug("Customer {} is seated at barber chair {}.", customerId, i);
                eventPublisher.publishEvent("customerSeated", customerId, i);
                break;
            }
        }
        barberRoomMutex.release();
    }

    /**
     * Notifies barbers that a customer is ready for a haircut.
     *
     * @param customerId The unique ID of the customer.
     */
    private void notifyBarbers(int customerId) {
        sleepyBarbers.release();
        LOG.debug("Customer {} notified the barbers.", customerId);
    }

    /**
     * Customer waits for their haircut to be completed.
     *
     * @param customerId The unique ID of the customer.
     * @return {@code true} when the haircut is completed.
     * @throws InterruptedException If the thread is interrupted while waiting for the haircut to finish.
     */
    private boolean waitForHaircutCompletion(int customerId) throws InterruptedException {
        int myChair = findCustomerChair(customerId);
        barberChairs[myChair].acquire(); // Wait for the barber to finish the haircut
        LOG.debug("Customer {} finished haircut at chair {}.", customerId, myChair);
        eventPublisher.publishEvent("customerFinished", customerId, myChair);
        barberChairSemaphore.release();
        return true;
    }

    /**
     * Finds the barber chair where the customer is seated.
     *
     * @param customerId The unique ID of the customer.
     * @return The chair index where the customer is seated.
     */
    private int findCustomerChair(int customerId) {
        for (int i = 0; i < numberOfBarbers; i++) {
            if (barberChairCustomerIds[i] == customerId) {
                return i;
            }
        }
        throw new IllegalStateException("Customer not found in any barber chair.");
    }

    /**
     * A barber begins their workday by serving customers.
     *
     * <p><b>Rationale:</b></p>
     * Each barber continuously waits for customers and serves them in a loop.
     * This process continues throughout their workday, ensuring that customers are handled as they arrive.
     *
     * @param barberId The unique ID of the barber.
     * @throws InterruptedException If the thread is interrupted while waiting for a customer.
     */
    public void barberEntry(int barberId) throws InterruptedException {
        LOG.info("Barber {} started working.", barberId);
        while (true) {
            waitForCustomer(barberId);
            int customerId = findNextCustomerForHaircut(barberId);
            performHaircut(barberId, customerId);
        }
    }

    /**
     * Barber waits for a customer to notify them.
     *
     * @param barberId The unique ID of the barber.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    private void waitForCustomer(int barberId) throws InterruptedException {
        LOG.debug("Barber {} is waiting for a customer.", barberId);
        sleepyBarbers.acquire();
    }

    /**
     * Finds the next customer for the barber to serve.
     *
     * @param barberId The unique ID of the barber.
     * @return The unique ID of the customer to be served.
     * @throws InterruptedException If the thread is interrupted.
     */
    private int findNextCustomerForHaircut(int barberId) throws InterruptedException {
        barberRoomMutex.acquire();
        for (int i = 0; i < numberOfBarbers; i++) {
            if (barberChairStates[i] == 1) { // Chair waiting for haircut
                barberChairStates[i] = 2; // Haircut in progress
                int customerId = barberChairCustomerIds[i];
                LOG.debug("Barber {} is starting haircut for customer {} at chair {}.", barberId, customerId, i);
                eventPublisher.publishEvent("barberStarted", barberId, i, customerId);
                barberRoomMutex.release();
                return customerId;
            }
        }
        barberRoomMutex.release();
        throw new IllegalStateException("No customer found for barber.");
    }

    /**
     * Performs the haircut and notifies the customer when finished.
     *
     * @param barberId   The unique ID of the barber.
     * @param customerId The unique ID of the customer receiving the haircut.
     * @throws InterruptedException If the thread is interrupted during the haircut.
     */
    private void performHaircut(int barberId, int customerId) throws InterruptedException {
        int chairId = findCustomerChair(customerId);
        cutHair(barberId, chairId);
        completeHaircut(barberId, customerId, chairId);
    }

    /**
     * Simulates the time taken to cut a customer's hair.
     *
     * @param barberId The unique ID of the barber.
     * @param chairId  The chair where the haircut is happening.
     * @throws InterruptedException If the thread is interrupted during the haircut.
     */
    private void cutHair(int barberId, int chairId) throws InterruptedException {
        LOG.debug("Barber {} is cutting hair at chair {}.", barberId, chairId);
        Thread.sleep(3000); // Simulate haircut duration
    }

    /**
     * Marks the haircut as complete and notifies the customer.
     *
     * @param barberId   The unique ID of the barber.
     * @param customerId The unique ID of the customer.
     * @param chairId    The chair where the haircut occurred.
     * @throws InterruptedException If the thread is interrupted while completing the haircut.
     */
    private void completeHaircut(int barberId, int customerId, int chairId) throws InterruptedException {
        barberRoomMutex.acquire();
        barberChairStates[chairId] = 0;
        barberChairCustomerIds[chairId] = -1;
        barberChairs[chairId].release(); // Notify the customer the haircut is complete
        LOG.debug("Barber {} finished haircut for customer {} at chair {}.", barberId, customerId, chairId);
        eventPublisher.publishEvent("barberFinished", barberId, chairId, customerId);
        barberRoomMutex.release();
    }


}
