package engine;

import java.util.List;

public final class TurnResult {
    private final boolean accepted;
    private final boolean turnConsumed;
    private final List<String> messages;
    private final GameOutcome outcome;

    private TurnResult(boolean accepted, boolean turnConsumed, List<String> messages, GameOutcome outcome) {
        this.accepted = accepted;
        this.turnConsumed = turnConsumed;
        this.messages = List.copyOf(messages);
        this.outcome = outcome;
    }

    public static TurnResult rejected(String message, GameOutcome outcome) {
        return new TurnResult(false, false, List.of(message), outcome);
    }

    public static TurnResult accepted(boolean turnConsumed, List<String> messages, GameOutcome outcome) {
        return new TurnResult(true, turnConsumed, messages, outcome);
    }

    public boolean isAccepted() { return accepted; }
    public boolean isTurnConsumed() { return turnConsumed; }
    public List<String> getMessages() { return messages; }
    public GameOutcome getOutcome() { return outcome; }
}