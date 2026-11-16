package console;

import java.util.Scanner;
import config.*;
import facade.GameEngineFacade;
import horror.HorrorEngine;
import static console.InputReader.*;

public final class ProjectEditor {
    private ProjectEditor() { }

    public static GameProject configureProject(Scanner scanner) {
        while (true) {
            String genre = readInt(scanner, "Choose genre:\n1. Horror\n2. Action", 1, 2) == 1 ? "HORROR" : "ACTION";
            String dimension = readInt(scanner, "Choose dimension:\n1. 2D\n2. 3D", 1, 2) == 1 ? "2D" : "3D";
            GameProjectBuilder builder = new GameProjectBuilder()
                    .setGenre(genre).setDimension(dimension).setName(readText(scanner, "Project name:"));
            try {
                if ("ACTION".equals(genre)) {
                    configureAction(scanner, builder);
                } else {
                    configureHorror(scanner, builder);
                }
                GameProject project = builder.build();
                GameEngineFacade.validateSimulation(project);
                return project;
            } catch (IllegalArgumentException exception) {
                System.out.println("Cannot build project: " + exception.getMessage());
                System.out.println("Please configure the project again.");
            }
        }
    }

    private static void configureAction(Scanner scanner, GameProjectBuilder builder) {
        builder.setPlayerHealth(readInt(scanner, "Player maximum HP:", 1, 200));
        int types = readInt(scanner, "Number of enemy types (1–5):", 1, 5);
        int remaining = 5;
        for (int i = 0; i < types; i++) {
            System.out.println("Enemy type " + (i + 1));
            String name = readText(scanner, "Name:");
            int health = readInt(scanner, "Maximum HP (1–200):", 1, 200);
            String behavior = readChoice(scanner, "Behavior:", "AGGRESSIVE", "CAUTIOUS");
            int damage = readInt(scanner, "Damage (1–20):", 1, 20);
            int maximum = remaining - (types - i - 1);
            int minimum = i == types - 1 ? remaining : 1;
            int quantity = readInt(scanner, "Quantity:", minimum, maximum);
            builder.addActionEnemy(name, health, damage, behavior, quantity);
            remaining -= quantity;
            System.out.println("Configured " + quantity + " x " + name + ".");
        }
    }

    private static void configureHorror(Scanner scanner, GameProjectBuilder builder) {
        int remaining = readInt(scanner, "Total enemies including Pursuer (3–5):", 3, 5) - 1;
        boolean watcher = false;
        boolean distractor = false;
        while (remaining > 0) {
            String name = readText(scanner, "Encounter enemy name:");
            String behavior;
            while (true) {
                behavior = readChoice(scanner, "Behavior:", "WATCHER", "DISTRACTOR");
                if (remaining == 1 && ((!watcher && !"WATCHER".equals(behavior))
                        || (!distractor && !"DISTRACTOR".equals(behavior)))) {
                    System.out.println("The last encounter must use the missing behavior.");
                } else {
                    break;
                }
            }
            watcher |= "WATCHER".equals(behavior);
            distractor |= "DISTRACTOR".equals(behavior);
            int maximum = remaining - (watcher && distractor ? 0 : 1);
            int quantity = readInt(scanner, "Quantity:", 1, maximum);
            builder.addHorrorEnemy(name, behavior, quantity);
            remaining -= quantity;
            System.out.println("Configured " + quantity + " x " + name + ".");
        }
        String pursuerName = readText(scanner, "Global Pursuer name:");
        // Build a configuration snapshot to calculate the requirement from actual quantities.
        builder.setPursuer(pursuerName, Integer.MAX_VALUE);
        HorrorConfig config = builder.build().getHorrorConfig();
        int minimum = HorrorEngine.minimumTurns(config);
        int recommended = HorrorEngine.recommendedCountdown(config);
        System.out.println("Minimum required countdown: " + minimum);
        System.out.println("Recommended countdown: " + (recommended + 2) + "–" + (recommended + 4));
        builder.setPursuer(pursuerName, readInt(scanner, "Pursuer countdown:", minimum, Integer.MAX_VALUE));
    }

}

