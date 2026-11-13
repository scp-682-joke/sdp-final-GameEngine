import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import config.*;
import creation.*;
import dimension.*;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("GAME SCENARIO BUILDER");
            GameProject project = configureProject(scanner);
            runMenu(scanner, project);
        } catch (NoSuchElementException exception) {
            System.out.println("\nInput closed. Goodbye.");
        }
    }

    // Developer 2 can reuse the configuration workflow without the demo menu.
    public static GameProject configureProject(Scanner scanner) {
        while (true) {
            String genre = readInt(scanner, "Choose genre:\n1. Horror\n2. Action", 1, 2) == 1 ? "HORROR" : "ACTION";
            String dimension = readInt(scanner, "Choose dimension:\n1. 2D\n2. 3D", 1, 2) == 1 ? "2D" : "3D";
            GameProjectBuilder builder = new GameProjectBuilder()
                    .setGenre(genre).setDimension(dimension).setName(readText(scanner, "Project name:"));
            if ("ACTION".equals(genre)) {
                configureAction(scanner, builder);
            } else {
                configureHorror(scanner, builder);
            }
            try {
                return builder.build();
            } catch (IllegalArgumentException exception) {
                System.out.println("Cannot build project: " + exception.getMessage());
                System.out.println("Please configure the project again.");
            }
        }
    }

    private static void configureAction(Scanner scanner, GameProjectBuilder builder) {
        builder.setPlayerHealth(readInt(scanner, "Player HP:", 1, Integer.MAX_VALUE));
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
        builder.setPursuer(readText(scanner, "Global Pursuer name:"),
                readInt(scanner, "Pursuer countdown:", 1, Integer.MAX_VALUE));
    }

    private static void runMenu(Scanner scanner, GameProject project) {
        Random seeds = new Random();
        while (true) {
            int choice = readInt(scanner, "\n1. View project configuration\n2. Generate randomized scenario\n3. Generate again\n4. Exit", 1, 4);
            if (choice == 4) { return; }
            if (choice == 1) {
                printProject(project);
            } else {
                DimensionRules rules = "2D".equals(project.getDimension()) ? new Rules2D() : new Rules3D();
                ScenarioCreator creator = "ACTION".equals(project.getGenre())
                        ? new ActionScenarioCreator(rules) : new HorrorScenarioCreator(rules);
                long seed = seeds.nextLong();
                System.out.println("Generation seed: " + seed);
                printScenario(creator.create(project, seed));
            }
        }
    }

    private static void printProject(GameProject project) {
        System.out.println(project.getName() + " | " + project.getGenre() + " | " + project.getDimension());
        if ("ACTION".equals(project.getGenre())) {
            System.out.println("Player HP: " + project.getActionConfig().getPlayerHealth());
            for (ActionEnemySpec spec : project.getActionConfig().getEnemies()) {
                System.out.println(describe(spec));
            }
        } else {
            for (HorrorEnemySpec spec : project.getHorrorConfig().getEnemies()) {
                System.out.println(spec.getName() + " | " + spec.getBehavior() + " | quantity=" + spec.getQuantity());
            }
            System.out.println("Global Pursuer: " + project.getHorrorConfig().getPursuerName()
                    + " | countdown=" + project.getHorrorConfig().getPursuerCountdown());
        }
    }

    private static void printScenario(GameScenario scenario) {
        printProject(scenario.getProject());
        if (scenario instanceof ActionScenario action) {
            for (ActionScenario.Building building : action.getBuildings()) {
                System.out.println("Building " + building.getNumber());
                for (ActionScenario.EnemyInstance enemy : building.getEnemies()) {
                    System.out.println("  " + describe(enemy.getSpecification()) + " | current HP=" + enemy.getCurrentHealth());
                }
            }
            System.out.println("Available interactions: " + action.getAvailableInteractions());
        } else if (scenario instanceof HorrorScenario horror) {
            int number = 1;
            for (HorrorScenario.EncounterRoom room : horror.getRooms()) {
                System.out.println("Room " + number++ + ": " + room.getSpecification().getName()
                        + " | " + room.getSpecification().getBehavior());
            }
            System.out.println("Available interactions: " + horror.getAvailableInteractions());
        }
    }

    private static String describe(ActionEnemySpec spec) {
        return spec.getName() + " | max HP=" + spec.getMaxHealth() + " | damage=" + spec.getDamage()
                + " | " + spec.getBehavior() + " | configured quantity=" + spec.getQuantity();
    }

    private static String readText(Scanner scanner, String prompt) {
        System.out.println(prompt);
        return scanner.nextLine();
    }

    private static int readInt(Scanner scanner, String prompt, int minimum, int maximum) {
        while (true) {
            String text = readText(scanner, prompt + " [" + minimum + "–" + maximum + "]");
            try {
                int value = Integer.parseInt(text.trim());
                if (value >= minimum && value <= maximum) { return value; }
            } catch (NumberFormatException exception) {
                // Show the same concise input guidance for malformed and out-of-range values.
            }
            System.out.println("Enter a whole number from " + minimum + " to " + maximum + ".");
        }
    }

    private static String readChoice(Scanner scanner, String prompt, String first, String second) {
        while (true) {
            String value = readText(scanner, prompt + " " + first + " or " + second);
            if (first.equals(value) || second.equals(value)) { return value; }
            System.out.println("Enter " + first + " or " + second + ".");
        }
    }
}
