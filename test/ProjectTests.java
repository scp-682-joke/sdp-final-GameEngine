package com.team.game;

import java.util.ArrayList;
import java.util.List;
import config.*;
import creation.*;
import com.team.game.dimension.DimensionRules.ActionInteraction;
import com.team.game.dimension.DimensionRules.HorrorInteraction;

/** Plain Java runner: failures throw AssertionError, even without -ea. */
public final class ProjectTests {
    private static int checks;

    public static void main(String[] args) {
        testValidation();
        testSnapshots();
        for (DimensionRules rules : List.of(new Rules2D(), new Rules3D())) {
            testAction(rules);
            testHorror(rules);
        }
        rejects(() -> new ActionScenarioCreator(new Rules2D()).create(horror("2D").build(), 0));
        rejects(() -> new HorrorScenarioCreator(new Rules2D()).create(action("2D").build(), 0));
        rejects(() -> new ActionScenarioCreator(new Rules3D()).create(action("2D").build(), 0));
        rejects(() -> new ActionScenarioCreator(new Rules2D()).create(null, 0));
        System.out.println("PASS: " + checks + " checks (both genres and both dimensions).");
    }

    private static GameProjectBuilder action(String dimension) {
        return new GameProjectBuilder().setName("Laboratory").setGenre("ACTION").setDimension(dimension)
                .setPlayerHealth(100).addActionEnemy("Soldier", 50, 12, "AGGRESSIVE", 3)
                .addActionEnemy("Guardian", 90, 15, "CAUTIOUS", 2);
    }

    private static GameProjectBuilder horror(String dimension) {
        return new GameProjectBuilder().setName("Mansion").setGenre("HORROR").setDimension(dimension)
                .addHorrorEnemy("Eyes", "WATCHER", 2).addHorrorEnemy("Noise", "DISTRACTOR", 1)
                .setPursuer("Shadow", 20);
    }

    private static GameProjectBuilder actionBase() {
        return new GameProjectBuilder().setName("A").setGenre("ACTION").setDimension("2D").setPlayerHealth(1);
    }

    private static GameProjectBuilder horrorBase() {
        return new GameProjectBuilder().setName("H").setGenre("HORROR").setDimension("2D");
    }

    private static void testValidation() {
        rejects(() -> new GameProjectBuilder().build());
        rejects(() -> action("2D").setName(" ").build());
        rejects(() -> action("2D").setGenre("RACING").build());
        rejects(() -> action("4D").build());
        rejects(() -> action("2D").setPlayerHealth(0).build());
        rejects(() -> actionBase().build());
        rejects(() -> actionBase().addActionEnemy("A", 1, 1, "AGGRESSIVE", 4).build());
        rejects(() -> actionBase().addActionEnemy("A", 1, 1, "AGGRESSIVE", 6).build());
        rejects(() -> actionBase().addActionEnemy("A", 1, 1, "AGGRESSIVE", 0).build());
        rejects(() -> actionBase().addActionEnemy("A", 1, 1, "AGGRESSIVE", -1).build());
        rejects(() -> actionBase().addActionEnemy("A", 1, 1, "AGGRESSIVE", Integer.MAX_VALUE).build());
        rejects(() -> actionBase().addActionEnemy("A", 0, 1, "AGGRESSIVE", 5).build());
        rejects(() -> actionBase().addActionEnemy("A", 201, 1, "AGGRESSIVE", 5).build());
        rejects(() -> actionBase().addActionEnemy("A", 1, 0, "AGGRESSIVE", 5).build());
        rejects(() -> actionBase().addActionEnemy("A", 1, 21, "AGGRESSIVE", 5).build());
        rejects(() -> actionBase().addActionEnemy("A", 1, 1, "PASSIVE", 5).build());
        rejects(() -> actionBase().addActionEnemy(" ", 1, 1, "CAUTIOUS", 5).build());
        rejects(() -> actionBase().addActionEnemy(null, 1, 1, "CAUTIOUS", 5).build());
        rejects(() -> actionBase().addActionEnemy("A", 1, 1, "CAUTIOUS", 3)
                .addActionEnemy("A", 1, 1, "CAUTIOUS", 2).build());
        rejects(() -> action("2D").setPursuer("X", 1).build());
        rejects(() -> action("2D").addHorrorEnemy("X", "WATCHER", 1).build());
        rejects(() -> horror("2D").setPlayerHealth(1).build());
        rejects(() -> horror("2D").addActionEnemy("X", 1, 1, "CAUTIOUS", 5).build());
        rejects(() -> horrorBase().addHorrorEnemy("W", "WATCHER", 1)
                .addHorrorEnemy("D", "DISTRACTOR", 1).build());
        rejects(() -> horror("2D").setPursuer("Shadow", 0).build());
        rejects(() -> horror("2D").setPursuer("", 2).build());
        rejects(() -> horror("2D").setPursuer("Eyes", 2).build());
        rejects(() -> horror("2D").addHorrorEnemy("Eyes", "DISTRACTOR", 1).build());
        rejects(() -> horror("2D").addHorrorEnemy("X", "PURSUER", 1).build());
        rejects(() -> horror("2D").addHorrorEnemy("X", "WATCHER", 0).build());
        rejects(() -> horror("2D").addHorrorEnemy("X", "WATCHER", 2).build());
        rejects(() -> horrorBase().addHorrorEnemy("W", "WATCHER", 2).setPursuer("P", 1).build());
        rejects(() -> horrorBase().addHorrorEnemy("D", "DISTRACTOR", 2).setPursuer("P", 1).build());
        check(actionBase().addActionEnemy("A", 200, 20, "CAUTIOUS", 5).build() != null, "Upper bounds accepted");
        check(horrorBase().addHorrorEnemy("W", "WATCHER", 1).addHorrorEnemy("D", "DISTRACTOR", 1)
                .setPursuer("P", 1).build() != null, "Minimum Horror total accepted");
        check(horror("2D").addHorrorEnemy("Other", "WATCHER", 1).build() != null, "Maximum Horror total accepted");
    }

    private static void testSnapshots() {
        GameProjectBuilder builder = action("2D");
        GameProject project = builder.build();
        builder.setName("Changed").setPlayerHealth(2).addActionEnemy("Extra", 1, 1, "CAUTIOUS", 1);
        check(project.getName().equals("Laboratory") && project.getActionConfig().getPlayerHealth() == 100
                && project.getActionConfig().getEnemies().size() == 2, "Action builder snapshot");
        immutable(() -> project.getActionConfig().getEnemies().clear());
        builder = horror("2D");
        GameProject horror = builder.build();
        builder.setPursuer("Replacement", 3).addHorrorEnemy("Extra", "WATCHER", 1);
        check(horror.getHorrorConfig().getPursuerName().equals("Shadow")
                && horror.getHorrorConfig().getPursuerCountdown() == 20
                && horror.getHorrorConfig().getEnemies().size() == 2, "Horror builder snapshot");
        immutable(() -> horror.getHorrorConfig().getEnemies().clear());
    }

    private static void testAction(DimensionRules rules) {
        GameProject project = action(rules.getDimension()).build();
        ScenarioCreator creator = new ActionScenarioCreator(rules);
        for (long seed = 0; seed < 100; seed++) {
            ActionScenario first = (ActionScenario) creator.create(project, seed);
            ActionScenario second = (ActionScenario) creator.create(project, seed);
            check(actionLayout(first).equals(actionLayout(second)), "Action seed reproducibility");
            check(first.getBuildings().size() == 2, "Two buildings");
            int total = 0;
            int soldiers = 0;
            int guardians = 0;
            for (int b = 0; b < 2; b++) {
                ActionScenario.Building building = first.getBuildings().get(b);
                check(!building.getEnemies().isEmpty(), "Nonempty building");
                check(building != second.getBuildings().get(b), "Independent building");
                for (int i = 0; i < building.getEnemies().size(); i++) {
                    ActionScenario.EnemyInstance enemy = building.getEnemies().get(i);
                    ActionEnemySpec spec = enemy.getSpecification();
                    check(enemy != second.getBuildings().get(b).getEnemies().get(i), "Independent enemy");
                    check(enemy.getCurrentHealth() == spec.getMaxHealth(), "Initial HP preserved");
                    if (spec.getName().equals("Soldier")) {
                        soldiers++;
                        check(spec.getMaxHealth() == 50 && spec.getDamage() == 12
                                && spec.getBehavior().equals("AGGRESSIVE") && spec.getQuantity() == 3, "Soldier properties");
                    } else {
                        guardians++;
                        check(spec.getName().equals("Guardian") && spec.getMaxHealth() == 90 && spec.getDamage() == 15
                                && spec.getBehavior().equals("CAUTIOUS") && spec.getQuantity() == 2, "Guardian properties");
                    }
                    total++;
                }
            }
            check(total == 5 && soldiers == 3 && guardians == 2, "Exact configured quantities");
        }
        ActionScenario scenario = (ActionScenario) creator.create(project, 42);
        ActionScenario.EnemyInstance enemy = scenario.getBuildings().get(0).getEnemies().get(0);
        enemy.setCurrentHealth(0);
        for (ActionScenario.Building building : scenario.getBuildings()) {
            for (ActionScenario.EnemyInstance other : building.getEnemies()) {
                if (other != enemy) { check(other.getCurrentHealth() == other.getSpecification().getMaxHealth(), "Sibling HP independent"); }
            }
        }
        check(enemy.getSpecification().getMaxHealth() > 0, "HP update leaves specification unchanged");
        check(((ActionScenario) creator.create(project, 42)).getBuildings().get(0).getEnemies().get(0).getCurrentHealth() > 0,
                "Generation starts with fresh HP");
        rejects(() -> enemy.setCurrentHealth(-1));
        rejects(() -> enemy.setCurrentHealth(enemy.getSpecification().getMaxHealth() + 1));
        scenario.setPlayerHealth(0);
        check(project.getActionConfig().getPlayerHealth() == 100, "Player configuration unchanged");
        rejects(() -> scenario.setPlayerHealth(101));
        immutable(() -> scenario.getBuildings().clear());
        immutable(() -> scenario.getBuildings().get(0).getEnemies().clear());
        check(scenario.getAvailableInteractions().contains(ActionInteraction.NORMAL_ATTACK), "Normal attack available");
        check(scenario.getAvailableInteractions().contains(ActionInteraction.MOVING_EVASIVE_ATTACK)
                == rules.getDimension().equals("3D"), "Action 3D-only capability");
        immutable(() -> scenario.getAvailableInteractions().clear());
    }

    private static void testHorror(DimensionRules rules) {
        GameProject project = horror(rules.getDimension()).build();
        ScenarioCreator creator = new HorrorScenarioCreator(rules);
        for (long seed = 0; seed < 100; seed++) {
            HorrorScenario first = (HorrorScenario) creator.create(project, seed);
            HorrorScenario second = (HorrorScenario) creator.create(project, seed);
            check(horrorLayout(first).equals(horrorLayout(second)), "Horror seed reproducibility");
            check(first.getRooms().size() == 3, "One room per encounter, excludes Pursuer");
            int watchers = 0;
            int distractors = 0;
            for (int i = 0; i < first.getRooms().size(); i++) {
                HorrorScenario.EncounterRoom room = first.getRooms().get(i);
                check(room != second.getRooms().get(i), "Independent encounter room");
                HorrorEnemySpec spec = room.getSpecification();
                if (spec.getBehavior().equals("WATCHER")) {
                    watchers++;
                    check(spec.getName().equals("Eyes") && spec.getQuantity() == 2, "Watcher properties");
                } else {
                    distractors++;
                    check(spec.getName().equals("Noise") && spec.getBehavior().equals("DISTRACTOR")
                            && spec.getQuantity() == 1, "Distractor properties");
                }
            }
            check(watchers == 2 && distractors == 1, "Horror configured quantities");
            check(first.getPursuerName().equals("Shadow") && first.getRemainingCountdown() == 20, "Pursuer preserved");
            first.setRemainingCountdown(0);
            check(second.getRemainingCountdown() == 20 && project.getHorrorConfig().getPursuerCountdown() == 20,
                    "Countdown independent from other scenario and configuration");
        }
        HorrorScenario scenario = (HorrorScenario) creator.create(project, 42);
        rejects(() -> scenario.setRemainingCountdown(-1));
        rejects(() -> scenario.setRemainingCountdown(21));
        immutable(() -> scenario.getRooms().clear());
        check(scenario.getAvailableInteractions().containsAll(List.of(HorrorInteraction.WAIT, HorrorInteraction.HIDE,
                HorrorInteraction.ADVANCE, HorrorInteraction.SEARCH_OBJECT, HorrorInteraction.DISTRACT)), "Basic Horror capabilities");
        check(scenario.getAvailableInteractions().contains(HorrorInteraction.CHOOSE_HIDING_LOCATION)
                == rules.getDimension().equals("3D"), "Horror 3D-only capability");
        immutable(() -> scenario.getAvailableInteractions().clear());
    }

    private static List<String> actionLayout(ActionScenario scenario) {
        List<String> layout = new ArrayList<>();
        for (ActionScenario.Building building : scenario.getBuildings()) {
            for (ActionScenario.EnemyInstance enemy : building.getEnemies()) {
                layout.add(building.getNumber() + ":" + enemy.getSpecification().getName());
            }
        }
        return layout;
    }

    private static List<String> horrorLayout(HorrorScenario scenario) {
        List<String> layout = new ArrayList<>();
        for (HorrorScenario.EncounterRoom room : scenario.getRooms()) {
            layout.add(room.getSpecification().getName());
        }
        return layout;
    }

    private static void rejects(Runnable operation) {
        try { operation.run(); }
        catch (IllegalArgumentException exception) {
            check(exception.getMessage() != null && !exception.getMessage().isBlank(), "Meaningful validation error");
            return;
        }
        throw new AssertionError("Expected invalid input to be rejected.");
    }

    private static void immutable(Runnable operation) {
        try { operation.run(); }
        catch (UnsupportedOperationException exception) { checks++; return; }
        throw new AssertionError("Expected an immutable list.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
        checks++;
    }
}
