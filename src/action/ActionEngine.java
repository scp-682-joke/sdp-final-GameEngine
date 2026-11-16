package action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import dimension.ActionScenario;
import dimension.ActionScenario.Building;
import dimension.ActionScenario.EnemyInstance;
import dimension.DimensionRules.ActionInteraction;
import engine.GameAction;
import engine.GameEngine;
import engine.GameOutcome;
import engine.TurnResult;
import engine.ActionType;
import events.EventType;
import events.GameEvent;
import events.GameEventPublisher;
import events.StatisticsTracker;

public final class ActionEngine implements GameEngine {
    public static final int NORMAL_ATTACK_DAMAGE = 25;
    public static final int MOVING_ATTACK_DAMAGE = 15;
    public static final int DODGE_CHANCE_PERCENT = 40;
    private static final int NO_BUILDING = -1;

    private final ActionScenario scenario;
    private final GameEventPublisher publisher;
    private final Random random;
    private final List<List<ActionCombatant>> combatants = new ArrayList<>();
    private final boolean[] cleared;
    private int currentBuilding = NO_BUILDING;
    private int turnsUsed;
    private int damageReceived;
    private GameOutcome outcome = GameOutcome.IN_PROGRESS;
    private String completionType = "";

    public ActionEngine(ActionScenario scenario, GameEventPublisher publisher, Random random) {
        this.scenario = scenario;
        this.publisher = publisher;
        this.random = random;
        this.cleared = new boolean[scenario.getBuildings().size()];
        createCombatants();
    }

    private void createCombatants() {
        Map<String, Integer> perName = new HashMap<>();
        for (Building building : scenario.getBuildings()) {
            List<ActionCombatant> inBuilding = new ArrayList<>();
            for (EnemyInstance enemy : building.getEnemies()) {
                String name = enemy.getSpecification().getName();
                int number = perName.merge(name, 1, Integer::sum);
                inBuilding.add(new ActionCombatant(name + " #" + number, enemy));
            }
            combatants.add(inBuilding);
        }
    }

    @Override public ActionScenario getScenario() { return scenario; }
    @Override public GameOutcome getOutcome() { return outcome; }
    public int getPlayerHealth() { return scenario.getPlayerHealth(); }
    public int getTurnsUsed() { return turnsUsed; }
    public int getDamageReceived() { return damageReceived; }
    public String getCompletionType() { return completionType; }
    public int getCurrentBuildingIndex() { return currentBuilding; }
    public List<ActionCombatant> getCombatants(int buildingIndex) { return List.copyOf(combatants.get(buildingIndex)); }

    @Override
    public List<GameAction> getAvailableActions() {
        List<GameAction> actions = new ArrayList<>();
        if (outcome != GameOutcome.IN_PROGRESS) { return actions; }
        if (currentBuilding == NO_BUILDING) { addLevelChoices(actions); } else { addAttackChoices(actions); }
        actions.add(GameAction.of(ActionType.INSPECT, "Inspect"));
        actions.add(GameAction.of(ActionType.VIEW_STATUS, "View status"));
        return actions;
    }

    private void addLevelChoices(List<GameAction> actions) {
        for (int i = 0; i < cleared.length; i++) {
            if (!cleared[i]) {
                actions.add(new GameAction(ActionType.ENTER_BUILDING, i, "Enter " + buildingName(i)));
            }
        }
        if (clearedCount() >= 1) { actions.add(GameAction.of(ActionType.USE_ELEVATOR, "Use the elevator to escape")); }
    }

    private void addAttackChoices(List<GameAction> actions) {
        List<ActionCombatant> inBuilding = combatants.get(currentBuilding);
        for (int i = 0; i < inBuilding.size(); i++) {
            ActionCombatant enemy = inBuilding.get(i);
            if (!enemy.isAlive()) { continue; }
            for (ActionInteraction interaction : scenario.getAvailableInteractions()) {
                actions.add(attackChoice(interaction, i, enemy));
            }
        }
    }

    private GameAction attackChoice(ActionInteraction interaction, int index, ActionCombatant enemy) {
        if (interaction == ActionInteraction.MOVING_EVASIVE_ATTACK) {
            return new GameAction(ActionType.MOVING_ATTACK, index, "Moving attack " + enemy.getLabel());
        }
        return new GameAction(ActionType.ATTACK, index, "Attack " + enemy.getLabel());
    }

    @Override
    public TurnResult perform(GameAction action) {
        if (outcome != GameOutcome.IN_PROGRESS) {
            return TurnResult.rejected("The game is over; no further actions are allowed.", outcome);
        }
        if (action == null || !getAvailableActions().contains(action)) {
            return TurnResult.rejected("That action is not available right now.", outcome);
        }
        switch (action.getType()) {
            case ATTACK: return fight(action.getTarget(), NORMAL_ATTACK_DAMAGE, false);
            case MOVING_ATTACK: return fight(action.getTarget(), MOVING_ATTACK_DAMAGE, true);
            case ENTER_BUILDING: return enterBuilding(action.getTarget());
            case USE_ELEVATOR: return useElevator();
            case INSPECT: return info(inspect());
            default: return info(getStatus());
        }
    }

    private TurnResult info(String text) {
        return TurnResult.accepted(false, List.of(text), outcome);
    }

    private TurnResult enterBuilding(int index) {
        currentBuilding = index;
        return TurnResult.accepted(false, List.of("You enter " + buildingName(index) + ". You must defeat every enemy before leaving.",
                livingEnemyLines(index)), outcome);
    }

    private TurnResult useElevator() {
        completionType = clearedCount() == cleared.length ? "FULL CLEAR" : "EARLY ESCAPE";
        outcome = GameOutcome.VICTORY;
        publisher.publish(new GameEvent(EventType.SCENARIO_COMPLETED, 0, completionType));
        return TurnResult.accepted(false, List.of("You escape in the elevator. " + completionType + "!"), outcome);
    }

    private TurnResult fight(int targetIndex, int rawDamage, boolean moving) {
        List<String> messages = new ArrayList<>();
        turnsUsed++;
        ActionCombatant target = combatants.get(currentBuilding).get(targetIndex);
        int taken = target.receiveHit(rawDamage);
        messages.add("You " + (moving ? "strike while moving at " : "attack ") + target.getLabel() + " for " + taken
                + " damage (HP " + target.getEnemy().getCurrentHealth() + "/" + target.getEnemy().getSpecification().getMaxHealth() + ").");
        if (!target.isAlive()) { defeat(target, messages); }
        if (buildingIsEmpty(currentBuilding)) {
            clearBuilding(messages);
        } else {
            retaliate(moving, messages);
        }
        return TurnResult.accepted(true, messages, outcome);
    }

    private void defeat(ActionCombatant target, List<String> messages) {
        messages.add(target.getLabel() + " is defeated.");
        publisher.publish(new GameEvent(EventType.ENEMY_DEFEATED, buildingNumber(currentBuilding), target.getLabel() + " defeated"));
    }

    private void clearBuilding(List<String> messages) {
        cleared[currentBuilding] = true;
        messages.add(buildingName(currentBuilding) + " is cleared!");
        publisher.publish(new GameEvent(EventType.BUILDING_CLEARED, buildingNumber(currentBuilding), buildingName(currentBuilding) + " cleared"));
        currentBuilding = NO_BUILDING;
    }

    private void retaliate(boolean playerIsMoving, List<String> messages) {
        for (ActionCombatant enemy : combatants.get(currentBuilding)) {
            if (!enemy.isAlive()) { continue; }
            int damage = enemy.act();
            if (damage == 0) {
                messages.add(enemy.getLabel() + " defends.");
            } else if (playerIsMoving && random.nextInt(100) < DODGE_CHANCE_PERCENT) {
                messages.add("You dodge " + enemy.getLabel() + "'s attack!");
            } else if (hitPlayer(enemy, damage, messages)) {
                return;
            }
        }
        messages.add("Player HP: " + scenario.getPlayerHealth() + "/" + scenario.getProject().getActionConfig().getPlayerHealth());
    }

    private boolean hitPlayer(ActionCombatant enemy, int damage, List<String> messages) {
        int lost = Math.min(damage, scenario.getPlayerHealth());
        scenario.setPlayerHealth(scenario.getPlayerHealth() - lost);
        damageReceived += lost;
        messages.add(enemy.getLabel() + " hits you for " + lost + ".");
        if (scenario.getPlayerHealth() > 0) { return false; }
        outcome = GameOutcome.DEFEAT;
        messages.add("You have been defeated.");
        publisher.publish(new GameEvent(EventType.PLAYER_DEFEATED, 0, "Killed by " + enemy.getLabel()));
        return true;
    }

    @Override
    public String getStatus() {
        StringBuilder text = new StringBuilder("Player HP: " + scenario.getPlayerHealth() + "/"
                + scenario.getProject().getActionConfig().getPlayerHealth()
                + " | Buildings cleared: " + clearedCount() + "/" + cleared.length + " | Turns used: " + turnsUsed);
        if (currentBuilding != NO_BUILDING) {
            text.append("\nInside ").append(buildingName(currentBuilding)).append(":\n").append(livingEnemyLines(currentBuilding));
        }
        return text.toString();
    }

    private String inspect() {
        if (currentBuilding == NO_BUILDING) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < cleared.length; i++) {
                text.append(buildingName(i)).append(cleared[i] ? " (cleared)" : "").append(":\n").append(livingEnemyLines(i)).append('\n');
            }
            return text.toString().trim();
        }
        StringBuilder text = new StringBuilder();
        for (ActionCombatant enemy : combatants.get(currentBuilding)) {
            if (enemy.isAlive()) {
                text.append(enemy.describe()).append(" | damage ").append(enemy.getEnemy().getSpecification().getDamage())
                        .append(" | ").append(enemy.getEnemy().getSpecification().getBehavior())
                        .append(" | now ").append(enemy.getBehavior().currentStance()).append('\n');
            }
        }
        return text.toString().trim();
    }

    private String livingEnemyLines(int buildingIndex) {
        StringBuilder text = new StringBuilder();
        for (ActionCombatant enemy : combatants.get(buildingIndex)) {
            if (enemy.isAlive()) { text.append("  ").append(enemy.describe()).append('\n'); }
        }
        return text.length() == 0 ? "  (no enemies left)" : text.toString().stripTrailing();
    }

    @Override
    public String getResults(StatisticsTracker statistics) {
        String result = outcome == GameOutcome.IN_PROGRESS ? "IN PROGRESS"
                : outcome + (completionType.isEmpty() ? "" : " (" + completionType + ")");
        return "Final result: " + result
                + "\nTotal enemies defeated: " + statistics.getEnemiesDefeated() + "/"
                + combatants.stream().mapToInt(List::size).sum()
                + "\nBuildings cleared: " + statistics.getBuildingsCleared() + "/" + cleared.length
                + "\nTotal damage received: " + damageReceived
                + "\nRemaining player HP: " + scenario.getPlayerHealth() + "/" + scenario.getProject().getActionConfig().getPlayerHealth()
                + "\nTurns used: " + turnsUsed;
    }

    private boolean buildingIsEmpty(int index) {
        for (ActionCombatant enemy : combatants.get(index)) {
            if (enemy.isAlive()) { return false; }
        }
        return true;
    }

    private int clearedCount() {
        int count = 0;
        for (boolean isCleared : cleared) { if (isCleared) { count++; } }
        return count;
    }

    private int buildingNumber(int index) { return scenario.getBuildings().get(index).getNumber(); }

    private String buildingName(int index) {
        return "Building " + (char) ('A' + index);
    }
}
