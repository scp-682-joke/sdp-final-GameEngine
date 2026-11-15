package horror;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import config.GameProject;
import config.HorrorConfig;
import config.HorrorEnemySpec;
import dimension.HorrorScenario;
import dimension.HorrorScenario.EncounterRoom;
import engine.GameAction;
import engine.GameEngine;
import engine.GameOutcome;
import engine.TurnResult;
import engine.ActionType;
import events.EventType;
import events.GameEvent;
import events.GameEventPublisher;
import events.StatisticsTracker;

public final class HorrorEngine implements GameEngine {
    static final int WATCHER_MINIMUM_ACTIONS = 6;
    static final int DISTRACTOR_MINIMUM_ACTIONS = 3;
    static final int DISTRACTOR_WORST_CASE_ACTIONS = 5;
    static final int FINAL_ESCAPE_ACTIONS = 1;

    private final HorrorScenario scenario;
    private final GameEventPublisher publisher;
    private final Random random;
    private final List<HorrorBehavior> rooms = new ArrayList<>();
    private int turnsUsed;
    private GameOutcome outcome = GameOutcome.IN_PROGRESS;
    private String cause = "";

    public HorrorEngine(HorrorScenario scenario, GameEventPublisher publisher, Random random) {
        validateCountdown(scenario.getProject());
        this.scenario = scenario;
        this.publisher = publisher;
        this.random = random;
        for (EncounterRoom room : scenario.getRooms()) {
            rooms.add(HorrorBehavior.forName(room.getSpecification().getBehavior()));
        }
    }

    public static int minimumTurns(HorrorConfig config) {
        return countRooms(config, HorrorBehavior.WATCHER) * WATCHER_MINIMUM_ACTIONS
                + countRooms(config, HorrorBehavior.DISTRACTOR) * DISTRACTOR_MINIMUM_ACTIONS + FINAL_ESCAPE_ACTIONS;
    }

    public static int recommendedCountdown(HorrorConfig config) {
        return countRooms(config, HorrorBehavior.WATCHER) * WATCHER_MINIMUM_ACTIONS
                + countRooms(config, HorrorBehavior.DISTRACTOR) * DISTRACTOR_WORST_CASE_ACTIONS + FINAL_ESCAPE_ACTIONS;
    }

    public static void validateCountdown(GameProject project) {
        HorrorConfig config = project.getHorrorConfig();
        int minimum = minimumTurns(config);
        if (config.getPursuerCountdown() < minimum) {
            throw new IllegalArgumentException("Pursuer countdown " + config.getPursuerCountdown()
                    + " is too low: this scenario needs at least " + minimum + " turns. Recommended: "
                    + recommendedCountdown(config) + " (covers unlucky searches).");
        }
    }

    private static int countRooms(HorrorConfig config, String behavior) {
        int count = 0;
        for (HorrorEnemySpec spec : config.getEnemies()) {
            if (behavior.equals(spec.getBehavior())) { count += spec.getQuantity(); }
        }
        return count;
    }

    @Override public HorrorScenario getScenario() { return scenario; }
    @Override public GameOutcome getOutcome() { return outcome; }
    public int getTurnsUsed() { return turnsUsed; }
    public int getRemainingCountdown() { return scenario.getRemainingCountdown(); }
    public String getCause() { return cause; }
    public int getCurrentRoomIndex() { return firstUnclearedRoom(); }
    public HorrorBehavior getRoomBehavior(int index) { return rooms.get(index); }

    @Override
    public List<GameAction> getAvailableActions() {
        List<GameAction> actions = new ArrayList<>();
        if (outcome != GameOutcome.IN_PROGRESS) { return actions; }
        int room = firstUnclearedRoom();
        if (room < 0) {
            actions.add(GameAction.of(ActionType.FINAL_ESCAPE, "Make the final escape"));
        } else {
            actions.addAll(rooms.get(room).availableActions(scenario.getAvailableInteractions()));
        }
        actions.add(GameAction.of(ActionType.INSPECT, "Inspect room"));
        actions.add(GameAction.of(ActionType.VIEW_STATUS, "View status"));
        return actions;
    }

    @Override
    public TurnResult perform(GameAction action) {
        if (outcome != GameOutcome.IN_PROGRESS) {
            return TurnResult.rejected("The game is over; no further actions are allowed.", outcome);
        }
        if (action == null || !getAvailableActions().contains(action)) {
            return TurnResult.rejected("That action is not available right now.", outcome);
        }
        if (action.getType() == ActionType.INSPECT) { return info(inspect()); }
        if (action.getType() == ActionType.VIEW_STATUS) { return info(getStatus()); }
        if (action.getType() == ActionType.FINAL_ESCAPE) { return escape(); }
        return playRoomAction(action);
    }

    private TurnResult info(String text) { return TurnResult.accepted(false, List.of(text), outcome); }

    private TurnResult playRoomAction(GameAction action) {
        int roomIndex = firstUnclearedRoom();
        EncounterStep step = rooms.get(roomIndex).resolve(action, random);
        List<String> messages = new ArrayList<>(List.of(step.getMessage()));
        if (step.isFatal()) {
            lose(step.getMessage(), messages);
            return TurnResult.accepted(false, messages, outcome);
        }
        spendTime(step.getTimeCost());
        if (step.isRoomCleared()) {
            publisher.publish(new GameEvent(EventType.ROOM_CLEARED, roomIndex + 1, "Room " + (roomIndex + 1) + " cleared"));
        }
        if (scenario.getRemainingCountdown() == 0) {
            lose(scenario.getPursuerName() + " catches you before you escape.", messages);
        } else {
            messages.add("Countdown: " + scenario.getRemainingCountdown());
        }
        return TurnResult.accepted(true, messages, outcome);
    }

    private TurnResult escape() {
        spendTime(1);
        outcome = GameOutcome.VICTORY;
        publisher.publish(new GameEvent(EventType.SCENARIO_COMPLETED, 0, "Escaped with " + scenario.getRemainingCountdown() + " turns left"));
        return TurnResult.accepted(true, List.of("You reach the exit and escape!"), outcome);
    }

    private void spendTime(int cost) {
        int spent = Math.min(cost, scenario.getRemainingCountdown());
        scenario.setRemainingCountdown(scenario.getRemainingCountdown() - spent);
        turnsUsed += spent;
    }

    private void lose(String reason, List<String> messages) {
        outcome = GameOutcome.DEFEAT;
        cause = reason;
        messages.add("You have been defeated.");
        publisher.publish(new GameEvent(EventType.PLAYER_DEFEATED, 0, reason));
    }

    @Override
    public String getStatus() {
        int room = firstUnclearedRoom();
        String where = room < 0 ? "All rooms cleared: find the exit"
                : "Room " + (room + 1) + "/" + rooms.size() + " (" + roomName(room) + "): " + rooms.get(room).describeState();
        return where + "\n" + scenario.getPursuerName() + " countdown: " + scenario.getRemainingCountdown()
                + " | Turns used: " + turnsUsed;
    }

    private String inspect() {
        int room = firstUnclearedRoom();
        return room < 0 ? "The exit is just ahead." : roomName(room) + ": " + rooms.get(room).inspect(scenario.getAvailableInteractions());
    }

    @Override
    public String getResults(StatisticsTracker statistics) {
        String result = outcome == GameOutcome.IN_PROGRESS ? "IN PROGRESS" : outcome.toString();
        return "Final result: " + result
                + "\nRooms completed: " + statistics.getRoomsCleared() + "/" + rooms.size()
                + "\nTurns used: " + turnsUsed
                + "\nRemaining countdown: " + scenario.getRemainingCountdown()
                + (outcome == GameOutcome.DEFEAT ? "\nCause of defeat: " + cause : "");
    }

    private int firstUnclearedRoom() {
        for (int i = 0; i < rooms.size(); i++) {
            if (!rooms.get(i).isCleared()) { return i; }
        }
        return -1;
    }

    private String roomName(int index) { return scenario.getRooms().get(index).getSpecification().getName(); }
}