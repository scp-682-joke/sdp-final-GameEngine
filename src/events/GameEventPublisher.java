package events;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GameEventPublisher {
    private final List<GameEventListener> listeners = new ArrayList<>();

    public void subscribe(GameEventListener listener) {
        Objects.requireNonNull(listener, "Listener is required.");
        if (!listeners.contains(listener)) { listeners.add(listener); }
    }

    public void unsubscribe(GameEventListener listener) { listeners.remove(listener); }

    public int getListenerCount() { return listeners.size(); }

    public void publish(GameEvent event) {
        for (GameEventListener listener : new ArrayList<>(listeners)) { listener.onEvent(event); }
    }
}