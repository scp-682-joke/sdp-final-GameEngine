package console;

import config.*;
import dimension.*;
import horror.HorrorEngine;
import java.util.HashMap;
import java.util.Map;

public final class ProjectPrinter {
    private ProjectPrinter() { }

    public static void printProject(GameProject project) {
        System.out.println("\n=== PROJECT CONFIGURATION ===");
        System.out.println(project.getName() + " | " + project.getGenre() + " | " + project.getDimension());
        if ("ACTION".equals(project.getGenre())) {
            System.out.println("Player HP: " + project.getActionConfig().getPlayerHealth());
            for (ActionEnemySpec spec : project.getActionConfig().getEnemies()) {
                System.out.println(describe(spec));
            }
            System.out.println("Total enemies: 5/5");
        } else {
            for (HorrorEnemySpec spec : project.getHorrorConfig().getEnemies()) {
                System.out.println(spec.getName() + " | " + spec.getBehavior() + " | quantity=" + spec.getQuantity());
            }
            System.out.println("Global Pursuer: " + project.getHorrorConfig().getPursuerName()
                    + " | countdown=" + project.getHorrorConfig().getPursuerCountdown());
            System.out.println("Minimum turns needed: " + HorrorEngine.minimumTurns(project.getHorrorConfig())
                    + " | countdown covering unlucky searches: " + HorrorEngine.recommendedCountdown(project.getHorrorConfig()));
            int rooms = project.getHorrorConfig().getEnemies().stream().mapToInt(HorrorEnemySpec::getQuantity).sum();
            System.out.println("Total enemies: " + (rooms + 1) + " | Encounter rooms: " + rooms);
        }
    }

    public static void printScenario(GameScenario scenario) {
        printProject(scenario.getProject());
        Map<String, Integer> perName = new HashMap<>();
        if (scenario instanceof ActionScenario action) {
            for (ActionScenario.Building building : action.getBuildings()) {
                System.out.println("Building " + (char) ('A' + building.getNumber() - 1)
                        + " (" + building.getEnemies().size() + " enemies)");
                for (ActionScenario.EnemyInstance enemy : building.getEnemies()) {
                    ActionEnemySpec spec = enemy.getSpecification();
                    int number = perName.merge(spec.getName(), 1, Integer::sum);
                    System.out.println("  " + spec.getName() + " #" + number + " | HP=" + enemy.getCurrentHealth()
                            + "/" + spec.getMaxHealth() + " | damage=" + spec.getDamage() + " | " + spec.getBehavior());
                }
            }
            System.out.println("Available interactions: " + action.getAvailableInteractions());
        } else if (scenario instanceof HorrorScenario horror) {
            int number = 1;
            for (HorrorScenario.EncounterRoom room : horror.getRooms()) {
                String name = room.getSpecification().getName();
                int instance = perName.merge(name, 1, Integer::sum);
                System.out.println("Room " + number++ + ": " + name + " #" + instance
                        + " | " + room.getSpecification().getBehavior());
            }
            System.out.println("Available interactions: " + horror.getAvailableInteractions());
        }
    }

    private static String describe(ActionEnemySpec spec) {
        return spec.getName() + " | max HP=" + spec.getMaxHealth() + " | damage=" + spec.getDamage()
                + " | " + spec.getBehavior() + " | configured quantity=" + spec.getQuantity();
    }

}

