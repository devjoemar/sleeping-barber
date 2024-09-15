package com.devjoemar.sleeping_barber.event;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@code EventPublisher} class is responsible for publishing events in the system to external components.
 * It leverages Spring's {@code SimpMessagingTemplate} to send WebSocket messages to connected clients.
 *
 * <p><b>Rationale:</b></p>
 * In a concurrent system like the Sleeping Barber Problem, it's important to notify external systems (e.g., user interfaces, dashboards)
 * about the current state of the system (e.g., when customers enter, leave, or get served by barbers). The {@code EventPublisher}
 * provides a clean and decoupled way to publish events as they occur, allowing interested parties to subscribe to these events.
 *
 * <p><b>Why use {@code SimpMessagingTemplate}?</b></p>
 * - The {@code SimpMessagingTemplate} provides an abstraction over WebSocket messaging, making it easy to push real-time updates to the client side.
 * - It decouples the event-publishing logic from the rest of the application, ensuring separation of concerns.
 * - It allows multiple subscribers (e.g., web clients) to receive updates in real-time, improving the responsiveness and interactivity of the system.
 *
 * <p>Without a mechanism like this, users would not receive real-time feedback on the state of the barbershop (e.g., when chairs become available,
 * when a customer leaves, etc.), which would make the system less interactive and informative.</p>
 */
@Component
public class EventPublisher {

    /**
     * The {@code SimpMessagingTemplate} is used to send messages to WebSocket clients.
     * It abstracts the complexity of WebSocket communication and provides methods to broadcast events to subscribers.
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Constructs an instance of the {@code EventPublisher} class.
     *
     * @param messagingTemplate The {@code SimpMessagingTemplate} used for sending WebSocket messages to connected clients.
     */
    public EventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Publishes an event with a basic set of data, consisting of the event type and entity ID.
     *
     * <p>This method provides a simplified way to send events where only the type of event (e.g., "customerLeft")
     * and the related entity ID (e.g., customer or barber) are required.</p>
     *
     * <p><b>Rationale:</b></p>
     * - This method simplifies event publishing when only minimal data is required (e.g., a customer leaving or arriving at the shop).
     * - Overloaded methods allow for flexibility, preventing unnecessary complexity when not all event details are needed.
     *
     * @param eventType The type of event being published (e.g., "customerLeft", "barberStarted").
     * @param entityId  The ID of the entity associated with the event (e.g., customer or barber).
     */
    public void publishEvent(String eventType, int entityId) {
        publishEvent(eventType, entityId, -1, -1); // Delegates to the more detailed method
    }

    /**
     * Publishes an event with the event type, entity ID, and chair ID.
     *
     * <p>This method is useful when an event also involves a specific chair (e.g., a customer sits at a chair for a haircut).</p>
     *
     * <p><b>Rationale:</b></p>
     * - By providing a method to specify a chair, this overload is useful for events where the barber chair is involved,
     * such as a customer being seated or a haircut starting.
     *
     * @param eventType The type of event being published.
     * @param entityId  The ID of the entity associated with the event.
     * @param chairId   The ID of the barber chair involved in the event (e.g., customer sits at chair).
     */
    public void publishEvent(String eventType, int entityId, int chairId) {
        publishEvent(eventType, entityId, chairId, -1); // Delegates to the method with all details
    }

    /**
     * Publishes an event with the event type, entity ID, chair ID, and customer ID.
     *
     * <p>This method provides the most detailed event information, allowing multiple pieces of data to be sent to clients.
     * It is useful for events where both a barber chair and a customer are involved (e.g., a customer sitting at a chair).</p>
     *
     * <p><b>Rationale:</b></p>
     * - Provides maximum flexibility by allowing the inclusion of all relevant information in the event.
     * - By specifying the customer and chair, this method is ideal for scenarios where both entities are involved in the event.
     * - Using this method ensures that the client receives all the necessary context about the event, allowing them to update their state accordingly.
     *
     * <p>Without this method, certain events (e.g., haircut starts) would lack important context, such as which customer is sitting in which chair,
     * making it harder for clients to properly display or process the event.</p>
     *
     * @param eventType  The type of event being published (e.g., "customerSeated", "barberStarted").
     * @param entityId   The ID of the entity associated with the event (e.g., barber or customer).
     * @param chairId    The ID of the barber chair involved in the event (if applicable).
     * @param customerId The ID of the customer associated with the event (if applicable).
     */
    public void publishEvent(String eventType, int entityId, int chairId, int customerId) {
        // Create a map to store the event details
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("entityId", entityId);
        event.put("chairId", chairId);
        event.put("customerId", customerId);

        // Send the event to WebSocket clients using the /topic/events destination
        messagingTemplate.convertAndSend("/topic/events", event);
    }
}
