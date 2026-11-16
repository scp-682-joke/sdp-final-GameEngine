import java.util.List;
import java.util.Random;
import action.*;
import config.*;
import dimension.*;
import engine.*;
import events.*;
import facade.GameEngineFacade;
import horror.*;

/** Plain Java regression runner; no assertion flags or external dependencies. */
public final class GameplayTests {
    private static int checks;

    public static void main(String[] args) {
        strategies();
        observers();
        for (String dimension : List.of("2D", "3D")) {
            actionGame(dimension, false);
            actionGame(dimension, true);
            horrorGame(dimension);
        }
        failures();
        countdownBoundary(10, GameOutcome.VICTORY);
        countdownBoundary(11, GameOutcome.DEFEAT);
        System.out.println("PASS: " + checks + " gameplay checks.");
    }

    private static GameProject action(String dimension, int hp, int enemyHp) {
        return new GameProjectBuilder().setName("Action").setGenre("ACTION").setDimension(dimension)
                .setPlayerHealth(hp).addActionEnemy("Guard", enemyHp, 8, "AGGRESSIVE", 3)
                .addActionEnemy("Defender", enemyHp, 12, "CAUTIOUS", 2).build();
    }

    private static GameProject horror(String dimension, int countdown) {
        return new GameProjectBuilder().setName("Horror").setGenre("HORROR").setDimension(dimension)
                .addHorrorEnemy("Eyes", "WATCHER", 1).addHorrorEnemy("Noise", "DISTRACTOR", 1)
                .setPursuer("Shadow", countdown).build();
    }

    private static void strategies() {
        EnemyBehavior aggressive = new AggressiveBehavior();
        check(aggressive.act(12) == 12 && aggressive.act(12) == 12, "Aggressive attacks every turn");
        CautiousBehavior cautious = new CautiousBehavior();
        check(cautious.adjustIncomingDamage(25) == 18, "Defending damage rounds up");
        check(cautious.act(12) == 0 && cautious.adjustIncomingDamage(25) == 25, "Defend then expose");
        check(cautious.act(12) == 12 && cautious.currentStance() == EnemyStance.DEFENDING, "Attack then defend");
        check(new CautiousBehavior().currentStance() == EnemyStance.DEFENDING, "Independent strategy state");
        DistractorBehavior distractor = new DistractorBehavior();
        Random unlucky = new Random(0) {
            @Override public int nextInt(int bound) { return bound - 1; }
        };
        for (int i = 0; i < 3; i++) { distractor.resolve(GameAction.of(ActionType.SEARCH, ""), unlucky); }
        check(distractor.isItemFound() && distractor.getSearchAttempts() == 3, "Third search guaranteed");
    }

    private static void observers() {
        GameEventPublisher publisher = new GameEventPublisher();
        StatisticsTracker tracker = new StatisticsTracker();
        publisher.subscribe(tracker);
        publisher.subscribe(tracker);
        publisher.publish(new GameEvent(EventType.ENEMY_DEFEATED, 1, "defeated"));
        check(tracker.getEnemiesDefeated() == 1 && publisher.getListenerCount() == 1, "No duplicate subscriptions");
        publisher.unsubscribe(tracker);
        publisher.publish(new GameEvent(EventType.ENEMY_DEFEATED, 2, "defeated"));
        check(tracker.getEnemiesDefeated() == 1, "Unsubscribe works");
    }

    private static void actionGame(String dimension, boolean early) {
        GameEngineFacade facade = new GameEngineFacade();
        GameProject project = action(dimension, 200, 30);
        ActionScenario preview = (ActionScenario) facade.generateScenario(project, 42);
        facade.startSimulation(project, 42);
        check(((ActionScenario) facade.getScenario()).getBuildings().get(0).getEnemies().size()
                == preview.getBuildings().get(0).getEnemies().size(), "Preview seed preserved");
        int[] endings = {0};
        facade.getPublisher().subscribe(event -> {
            if (event.getType() == EventType.SCENARIO_COMPLETED) { endings[0]++; }
        });
        check(!facade.performAction(GameAction.of(ActionType.USE_ELEVATOR, "")).isAccepted(), "No immediate escape");
        int guard = 0;
        while (!facade.isFinished() && guard++ < 100) {
            if (has(facade, ActionType.ATTACK)) {
                check(has(facade, ActionType.MOVING_ATTACK) == dimension.equals("3D"), "Bridge attack capabilities");
                check(!act(facade, ActionType.INSPECT).isTurnConsumed(), "Inspection is free");
                act(facade, dimension.equals("3D") ? ActionType.MOVING_ATTACK : ActionType.ATTACK);
            } else if (has(facade, ActionType.USE_ELEVATOR) && (early || !has(facade, ActionType.ENTER_BUILDING))) {
                act(facade, ActionType.USE_ELEVATOR);
            } else { act(facade, ActionType.ENTER_BUILDING); }
        }
        check(facade.getOutcome() == GameOutcome.VICTORY, "Action victory");
        check(facade.getStatistics().getBuildingsCleared() == (early ? 1 : 2), "Building progression");
        check(facade.getResults().contains(early ? "EARLY ESCAPE" : "FULL CLEAR"), "Completion type");
        check(!facade.performAction(GameAction.of(ActionType.USE_ELEVATOR, "")).isAccepted() && endings[0] == 1,
                "Terminal event published once");
        facade.restartSimulation(42);
        check(facade.getStatistics().getEnemiesDefeated() == 0 && facade.getPublisher().getListenerCount() == 2,
                "Restart resets state and observers");
    }

    private static void horrorGame(String dimension) {
        GameEngineFacade facade = new GameEngineFacade();
        facade.startSimulation(horror(dimension, 12), 42);
        int guard = 0;
        while (!facade.isFinished() && guard++ < 30) {
            check(!act(facade, ActionType.VIEW_STATUS).isTurnConsumed(), "Status is free");
            if (has(facade, ActionType.HIDE_WAIT)) { act(facade, ActionType.HIDE_WAIT); }
            else if (has(facade, ActionType.HIDE_CLOSET)) { act(facade, ActionType.HIDE_CLOSET); }
            else if (has(facade, ActionType.SEARCH)) { act(facade, ActionType.SEARCH); }
            else if (has(facade, ActionType.USE_ITEM)) { act(facade, ActionType.USE_ITEM); }
            else if (has(facade, ActionType.FINAL_ESCAPE)) { act(facade, ActionType.FINAL_ESCAPE); }
            else { act(facade, ActionType.GO_FORWARD); }
        }
        check(facade.getOutcome() == GameOutcome.VICTORY && facade.getStatistics().getRoomsCleared() == 2,
                "Complete Horror game within recommended countdown");
        check(!facade.performAction(GameAction.of(ActionType.FINAL_ESCAPE, "")).isAccepted(), "Horror terminal guard");
    }

    private static void failures() {
        GameEngineFacade facade = new GameEngineFacade();
        facade.startSimulation(action("2D", 1, 200), 42);
        act(facade, ActionType.ENTER_BUILDING);
        for (int i = 0; i < 3 && !facade.isFinished(); i++) { act(facade, ActionType.ATTACK); }
        check(facade.getOutcome() == GameOutcome.DEFEAT, "Combat defeat");
        facade.startSimulation(horror("3D", 12), 42);
        act(facade, ActionType.GO_FORWARD);
        check(facade.getOutcome() == GameOutcome.DEFEAT, "Unsafe Horror advance is fatal");
        try {
            facade.startSimulation(horror("2D", 1), 42);
            throw new AssertionError("Impossible countdown accepted");
        } catch (IllegalArgumentException expected) {
            check(facade.getOutcome() == GameOutcome.DEFEAT, "Failed start preserves previous session");
        }
        try {
            facade.generateScenario(null, 0);
            throw new AssertionError("Null project accepted");
        } catch (IllegalArgumentException expected) { checks++; }
    }

    private static void countdownBoundary(int countdown, GameOutcome expected) {
        HorrorScenario scenario = (HorrorScenario) new GameEngineFacade().generateScenario(horror("2D", countdown), 42);
        Random searches = new Random(0) {
            @Override public int nextInt(int bound) { return expected == GameOutcome.VICTORY ? 0 : bound - 1; }
        };
        HorrorEngine engine = new HorrorEngine(scenario, new GameEventPublisher(), searches);
        TurnResult last = null;
        for (int i = 0; i < 20 && engine.getOutcome() == GameOutcome.IN_PROGRESS; i++) {
            HorrorBehavior behavior = engine.getCurrentRoomIndex() < 0 ? null : engine.getRoomBehavior(engine.getCurrentRoomIndex());
            ActionType type;
            if (behavior == null) { type = ActionType.FINAL_ESCAPE; }
            else if (behavior instanceof WatcherBehavior watcher) {
                type = watcher.isAlert() ? ActionType.HIDE_WAIT : ActionType.GO_FORWARD;
            } else {
                DistractorBehavior distractor = (DistractorBehavior) behavior;
                type = !distractor.isItemFound() ? ActionType.SEARCH
                        : !distractor.isDistracted() ? ActionType.USE_ITEM : ActionType.GO_FORWARD;
            }
            last = engine.perform(GameAction.of(type, ""));
        }
        check(engine.getOutcome() == expected && engine.getRemainingCountdown() == 0, "Countdown boundary");
        check(engine.getTurnsUsed() == countdown, "Countdown accounting");
        if (expected == GameOutcome.DEFEAT) {
            check(last.getMessages().stream().anyMatch(message -> message.contains("Shadow catches")),
                    "Countdown defeat explains its cause immediately");
        }
    }

    private static boolean has(GameEngineFacade facade, ActionType type) {
        return facade.getAvailableActions().stream().anyMatch(action -> action.getType() == type);
    }

    private static TurnResult act(GameEngineFacade facade, ActionType type) {
        return facade.performAction(facade.getAvailableActions().stream()
                .filter(action -> action.getType() == type).findFirst().orElseThrow());
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
        checks++;
    }
}
