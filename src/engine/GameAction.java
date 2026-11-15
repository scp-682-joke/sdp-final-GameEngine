package engine;

import java.util.Objects;

public final class GameAction {
    public static final int NO_TARGET = -1;

    private final ActionType type;
    private final int target;
    private final String label;

    public GameAction(ActionType type, int target, String label) {
        this.type = Objects.requireNonNull(type, "Action type is required.");
        this.target = target;
        this.label = Objects.requireNonNull(label, "Action label is required.");
    }

    public static GameAction of(ActionType type, String label) { return new GameAction(type, NO_TARGET, label); }

    public ActionType getType() { return type; }
    public int getTarget() { return target; }
    public String getLabel() { return label; }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (!(other instanceof GameAction)) { return false; }
        GameAction action = (GameAction) other;
        return type == action.type && target == action.target;
    }

    @Override
    public int hashCode() { return Objects.hash(type, target); }

    @Override
    public String toString() { return label; }
}