package horror;

public final class EncounterStep {
    private final String message;
    private final int timeCost;
    private final boolean fatal;
    private final boolean roomCleared;

    private EncounterStep(String message, int timeCost, boolean fatal, boolean roomCleared) {
        this.message = message;
        this.timeCost = timeCost;
        this.fatal = fatal;
        this.roomCleared = roomCleared;
    }

    public static EncounterStep progress(String message, int timeCost) { return new EncounterStep(message, timeCost, false, false); }
    public static EncounterStep cleared(String message, int timeCost) { return new EncounterStep(message, timeCost, false, true); }
    public static EncounterStep fatal(String message) { return new EncounterStep(message, 0, true, false); }

    public String getMessage() { return message; }
    public int getTimeCost() { return timeCost; }
    public boolean isFatal() { return fatal; }
    public boolean isRoomCleared() { return roomCleared; }
}