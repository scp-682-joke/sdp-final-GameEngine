package events;

import java.util.Objects;

public final class GameEvent {
    private final EventType type;
    private final int number;
    private final String description;

    public GameEvent(EventType type, int number, String description) {
        this.type = Objects.requireNonNull(type, "Event type is required.");
        this.number = number;
        this.description = Objects.requireNonNull(description, "Event description is required.");
    }

    public EventType getType() { return type; }
    public int getNumber() { return number; }
    public String getDescription() { return description; }
}