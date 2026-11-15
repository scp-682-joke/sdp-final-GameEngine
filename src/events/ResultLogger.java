package events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ResultLogger implements GameEventListener {
    private final List<String> lines = new ArrayList<>();
    private String outcome;

    @Override
    public void onEvent(GameEvent event) {
        lines.add(event.getType() + ": " + event.getDescription());
        if (event.getType() == EventType.PLAYER_DEFEATED || event.getType() == EventType.SCENARIO_COMPLETED) {
            outcome = event.getType() + ": " + event.getDescription();
        }
    }

    public List<String> getLines() { return Collections.unmodifiableList(lines); }
    public String getOutcome() { return outcome; }
}