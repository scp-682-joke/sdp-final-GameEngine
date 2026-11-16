import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import action.*;
import config.*;
import console.*;
import dimension.*;
import engine.*;
import events.*;
import facade.GameEngineFacade;
import horror.*;

/** Controlled layouts/random rolls validate the reference without asserting fictional seed layouts. */
public final class AcceptanceTests {
    private static int checks;

    public static void main(String[] args) {
        actionWalkthrough();
        dodgeAndInvalidActions();
        horrorWalkthrough();
        horrorDefeats();
        consoleFlows();
        System.out.println("PASS: " + checks + " acceptance checks.");
    }

    private static GameProject actionProject(String dimension) {
        return new GameProjectBuilder().setName("Military Base").setGenre("ACTION").setDimension(dimension)
                .setPlayerHealth(100).addActionEnemy("Soldier", 30, 8, "AGGRESSIVE", 2)
                .addActionEnemy("Guardian", 35, 12, "CAUTIOUS", 1)
                .addActionEnemy("Mutant", 60, 15, "AGGRESSIVE", 2).build();
    }

    private static ActionScenario actionScenario(String dimension) {
        GameProject project = actionProject(dimension);
        List<ActionEnemySpec> specs = project.getActionConfig().getEnemies();
        return new ActionScenario(project, dimension.equals("3D") ? new Rules3D() : new Rules2D(), List.of(
                new ActionScenario.Building(1, List.of(new ActionScenario.EnemyInstance(specs.get(0)),
                        new ActionScenario.EnemyInstance(specs.get(1)))),
                new ActionScenario.Building(2, List.of(new ActionScenario.EnemyInstance(specs.get(0)),
                        new ActionScenario.EnemyInstance(specs.get(2)), new ActionScenario.EnemyInstance(specs.get(2))))));
    }

    private static TurnResult perform(GameEngine engine, ActionType type, int target) {
        return engine.perform(new GameAction(type, target, "test"));
    }

    private static TurnResult perform(GameEngine engine, ActionType type) {
        return perform(engine, type, GameAction.NO_TARGET);
    }

    private static void actionWalkthrough() {
        StatisticsTracker statistics = new StatisticsTracker();
        GameEventPublisher publisher = new GameEventPublisher();
        List<EventType> events = new ArrayList<>();
        publisher.subscribe(statistics);
        publisher.subscribe(event -> events.add(event.getType()));
        ActionEngine engine = new ActionEngine(actionScenario("3D"), publisher, new Rolls(0));
        perform(engine, ActionType.ENTER_BUILDING, 0);
        check(!perform(engine, ActionType.ENTER_BUILDING, 1).isAccepted(), "Cannot leave unfinished building");
        check(!perform(engine, ActionType.USE_ELEVATOR).isAccepted(), "Elevator locked during combat");
        perform(engine, ActionType.MOVING_ATTACK, 0);
        check(engine.getPlayerHealth() == 100 && engine.getCombatants(0).get(0).getEnemy().getCurrentHealth() == 15,
                "Moving attack: 15 damage, dodge, cautious defense");
        perform(engine, ActionType.ATTACK, 0);
        check(engine.getPlayerHealth() == 88 && statistics.getEnemiesDefeated() == 1, "Dead Soldier cannot retaliate; Guardian attacks");
        check(engine.getAvailableActions().stream().noneMatch(a -> a.getTarget() == 0 && a.getType() == ActionType.ATTACK),
                "Dead target removed from menu");
        perform(engine, ActionType.ATTACK, 1);
        check(engine.getCombatants(0).get(1).getEnemy().getCurrentHealth() == 17 && engine.getPlayerHealth() == 88,
                "Third turn defense reduces 25 to 18 and does not attack");
        perform(engine, ActionType.ATTACK, 1);
        check(engine.getTurnsUsed() == 4 && engine.getDamageReceived() == 12 && engine.getPlayerHealth() == 88,
                "Reference combat totals");
        check(statistics.getEnemiesDefeated() == 2 && statistics.getBuildingsCleared() == 1, "Actual combat statistics");
        perform(engine, ActionType.USE_ELEVATOR);
        perform(engine, ActionType.USE_ELEVATOR);
        check(engine.getOutcome() == GameOutcome.VICTORY && engine.getCompletionType().equals("EARLY ESCAPE"), "Early escape");
        check(Collections.frequency(events, EventType.SCENARIO_COMPLETED) == 1
                && Collections.frequency(events, EventType.ENEMY_DEFEATED) == 2, "Exactly one event per outcome");
    }

    private static void dodgeAndInvalidActions() {
        Rolls rolls = new Rolls(39, 40, 99);
        ActionEngine engine = new ActionEngine(actionScenario("3D"), new GameEventPublisher(), rolls);
        perform(engine, ActionType.ENTER_BUILDING, 1);
        perform(engine, ActionType.MOVING_ATTACK, 0);
        check(rolls.calls == 3 && engine.getDamageReceived() == 30, "Independent 40% boundary rolls for three attacks");
        ActionScenario scenario = actionScenario("2D");
        engine = new ActionEngine(scenario, new GameEventPublisher(), new Rolls());
        perform(engine, ActionType.ENTER_BUILDING, 1);
        check(!perform(engine, ActionType.MOVING_ATTACK, 0).isAccepted(), "2D rejects forged moving attack");
        check(!perform(engine, ActionType.ATTACK, 99).isAccepted() && !engine.perform(null).isAccepted(), "Invalid targets/null rejected");
        check(engine.getPlayerHealth() == 100 && engine.getTurnsUsed() == 0, "Invalid actions preserve state");
        scenario.setPlayerHealth(7);
        List<EventType> events = new ArrayList<>();
        GameEventPublisher publisher = new GameEventPublisher();
        publisher.subscribe(event -> events.add(event.getType()));
        engine = new ActionEngine(scenario, publisher, new Rolls());
        perform(engine, ActionType.ENTER_BUILDING, 1);
        perform(engine, ActionType.ATTACK, 1);
        perform(engine, ActionType.ATTACK, 1);
        check(engine.getPlayerHealth() == 0 && engine.getDamageReceived() == 7, "Damage clamps to actual HP lost");
        check(Collections.frequency(events, EventType.PLAYER_DEFEATED) == 1, "Action death notified once");
    }

    private static HorrorScenario horrorScenario(String dimension) {
        GameProject project = new GameProjectBuilder().setName("Haunted Hospital").setGenre("HORROR").setDimension(dimension)
                .addHorrorEnemy("Nurse", "WATCHER", 1).addHorrorEnemy("Janitor", "DISTRACTOR", 1)
                .setPursuer("Shadow", 16).build();
        return new HorrorScenario(project, dimension.equals("3D") ? new Rules3D() : new Rules2D(),
                project.getHorrorConfig().getEnemies().stream().map(HorrorScenario.EncounterRoom::new).toList());
    }

    private static void clearWatcher(HorrorEngine engine, ActionType hide) {
        for (int i = 0; i < 3; i++) {
            perform(engine, hide);
            perform(engine, ActionType.GO_FORWARD);
        }
    }

    private static void horrorWalkthrough() {
        GameEventPublisher publisher = new GameEventPublisher();
        StatisticsTracker statistics = new StatisticsTracker();
        List<EventType> events = new ArrayList<>();
        publisher.subscribe(statistics);
        publisher.subscribe(event -> events.add(event.getType()));
        HorrorEngine engine = new HorrorEngine(horrorScenario("3D"), publisher, new Rolls(99, 0));
        check(!perform(engine, ActionType.FINAL_ESCAPE).isAccepted(), "Cannot escape uncleared rooms");
        check(!perform(engine, ActionType.HIDE_WAIT).isAccepted(), "3D enforces hiding location");
        perform(engine, ActionType.INSPECT);
        perform(engine, ActionType.VIEW_STATUS);
        check(engine.getTurnsUsed() == 0 && engine.getRemainingCountdown() == 16, "Inspection/status/invalid actions are free");
        clearWatcher(engine, ActionType.HIDE_CLOSET);
        check(statistics.getRoomsCleared() == 1 && engine.getRemainingCountdown() == 10, "Three safe advances take exactly six turns");
        check(!perform(engine, ActionType.USE_ITEM).isAccepted(), "Cannot distract without item");
        perform(engine, ActionType.SEARCH);
        check(!((DistractorBehavior) engine.getRoomBehavior(1)).isItemFound(), "Search may fail");
        perform(engine, ActionType.SEARCH);
        perform(engine, ActionType.USE_ITEM);
        perform(engine, ActionType.GO_FORWARD);
        check(engine.getRemainingCountdown() == 6 && statistics.getRoomsCleared() == 2, "Distractor sequence costs four turns");
        perform(engine, ActionType.FINAL_ESCAPE);
        check(engine.getOutcome() == GameOutcome.VICTORY && engine.getTurnsUsed() == 11 && engine.getRemainingCountdown() == 5,
                "Exact Horror walkthrough totals");
        check(!perform(engine, ActionType.FINAL_ESCAPE).isAccepted()
                && Collections.frequency(events, EventType.SCENARIO_COMPLETED) == 1, "Horror victory notified once");
        GameEngineFacade facade = new GameEngineFacade();
        facade.startSimulation(horrorScenario("2D").getProject(), 42);
        facade.performAction(GameAction.of(ActionType.GO_FORWARD, ""));
        facade.restartSimulation(42);
        check(!facade.isFinished() && ((HorrorScenario) facade.getScenario()).getRemainingCountdown() == 16
                && facade.getStatistics().getRoomsCleared() == 0, "Horror replay starts fresh");
    }

    private static void horrorDefeats() {
        for (ActionType fatal : List.of(ActionType.HIDE_TABLE, ActionType.GO_FORWARD)) {
            GameEventPublisher publisher = new GameEventPublisher();
            List<EventType> events = new ArrayList<>();
            publisher.subscribe(event -> events.add(event.getType()));
            HorrorEngine engine = new HorrorEngine(horrorScenario("3D"), publisher, new Rolls());
            TurnResult result = perform(engine, fatal);
            check(result.isTurnConsumed() && engine.getTurnsUsed() == 1 && engine.getRemainingCountdown() == 15,
                    "Fatal Watcher action consumes one turn");
            check(engine.getOutcome() == GameOutcome.DEFEAT && engine.getAvailableActions().isEmpty(), "Watcher defeat stops play");
            perform(engine, fatal);
            check(Collections.frequency(events, EventType.PLAYER_DEFEATED) == 1, "Horror death notified once");
        }
        HorrorEngine engine = new HorrorEngine(horrorScenario("2D"), new GameEventPublisher(), new Rolls());
        check(!perform(engine, ActionType.HIDE_CLOSET).isAccepted(), "2D rejects specific hiding locations");
        clearWatcher(engine, ActionType.HIDE_WAIT);
        perform(engine, ActionType.GO_FORWARD);
        check(engine.getOutcome() == GameOutcome.DEFEAT && engine.getTurnsUsed() == 7, "Undistracted Janitor catches player");
        HorrorScenario scenario = horrorScenario("2D");
        scenario.setRemainingCountdown(1);
        engine = new HorrorEngine(scenario, new GameEventPublisher(), new Rolls());
        perform(engine, ActionType.HIDE_WAIT);
        check(engine.getRemainingCountdown() == 0 && engine.getTurnsUsed() == 1
                && engine.getOutcome() == GameOutcome.DEFEAT && engine.getCause().contains("Shadow"), "Pursuer catches on zero");
    }

    private static void consoleFlows() {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            for (int dimension : List.of(1, 2)) {
                GameProject project = ProjectEditor.configureProject(new Scanner("1\n" + dimension
                        + "\nHospital\n3\nNurse\n1\n1\nJanitor\n2\n1\nShadow\n3\nabc\n16\n"));
                check(project.getHorrorConfig().getPursuerCountdown() == 16, "Countdown retry retains project");
                check(project.getDimension().equals(dimension == 1 ? "2D" : "3D"), "Console dimension selection");
            }
            GameProject project = ProjectEditor.configureProject(new Scanner(
                    "2\n1\nBase\n201\n100\n1\nSoldier\n30\nBOTH\n1\n25\nabc\n8\n5\n"));
            check(project.getActionConfig().getPlayerHealth() == 100
                    && project.getActionConfig().getEnemies().get(0).getDamage() == 8, "Local HP/damage/behavior retries");
            GameProject lethal = new GameProjectBuilder().setName("Replay").setGenre("ACTION").setDimension("2D")
                    .setPlayerHealth(1).addActionEnemy("Guard", 200, 20, "AGGRESSIVE", 5).build();
            new ConsoleController().runMenu(new Scanner("4\n99\nabc\n1\n1\n1\n1\n1\n3\n"), lethal);
            String transcript = output.toString(StandardCharsets.UTF_8);
            check(transcript.contains("Minimum required countdown: 10"), "Calculated minimum shown before countdown input");
            check(transcript.contains("Recommended countdown: 14–16"), "Recommendation includes search allowance and margin");
            check(transcript.split("=== RESULTS ===", -1).length - 1 == 2, "Console completes two sessions and exits");
            check(transcript.contains("Invalid input. Please enter an integer."), "Malformed input explained");
            // More Watchers must change the minimum rather than reuse ten.
            GameProject larger = ProjectEditor.configureProject(new Scanner(
                    "1\n1\nLarge\n4\nEyes\n1\n2\nNoise\n2\n1\nShadow\n10\n20\n"));
            check(HorrorEngine.minimumTurns(larger.getHorrorConfig()) == 16, "Countdown minimum scales with room quantities");
            // Duplicate names during the temporary countdown snapshot must be handled by the editor.
            ProjectEditor.configureProject(new Scanner("1\n1\nBad\n3\nSame\n1\n1\nSame\n2\n1\nShadow\n"
                    + "2\n1\nRecovered\n100\n1\nGuard\n1\n1\n1\n5\n"));
            check(output.toString(StandardCharsets.UTF_8).contains("Cannot build project:"), "Invalid whole project retries without crash");
        } finally { System.setOut(original); }
    }

    private static final class Rolls extends Random {
        private static final long serialVersionUID = 1L;
        private final int[] values;
        private int calls;
        Rolls(int... values) { this.values = values; }
        @Override public int nextInt(int bound) {
            if (calls >= values.length) { throw new AssertionError("Unexpected random roll"); }
            return values[calls++];
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
        checks++;
    }
}
