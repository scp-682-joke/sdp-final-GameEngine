package console;

import java.util.List;
import java.util.Random;
import java.util.Scanner;
import config.GameProject;
import engine.GameAction;
import engine.TurnResult;
import facade.GameEngineFacade;
import engine.ActionType;

final class GameConsole {
    private final Scanner scanner;
    private final Random seeds;
    private final GameEngineFacade facade;

    GameConsole(Scanner scanner, Random seeds, GameEngineFacade facade) {
        this.scanner = scanner;
        this.seeds = seeds;
        this.facade = facade;
    }

    boolean play(GameProject project, Long generationSeed) {
        do {
            if (!startSession(project, generationSeed)) { return false; }
            generationSeed = null;
            runSession();
            System.out.println("\n=== RESULTS ===\n" + facade.getResults());
            int choice = InputReader.readInt(scanner,
                    "\n1. Generate a new scenario and play again\n2. Back to project menu\n3. Exit", 1, 3);
            if (choice != 1) { return choice == 3; }
        } while (true);
    }

    private boolean startSession(GameProject project, Long generationSeed) {
        long seed = generationSeed == null ? seeds.nextLong() : generationSeed;
        try {
            facade.startSimulation(project, seed);
        } catch (IllegalArgumentException exception) {
            System.out.println("Cannot start simulation: " + exception.getMessage());
            return false;
        }
        System.out.println("\n=== SIMULATION STARTED (seed " + seed + ") ===");
        ProjectPrinter.printScenario(facade.getScenario());
        showOverview();
        return true;
    }

    private void showOverview() {
        for (GameAction action : facade.getAvailableActions()) {
            if (action.getType() == ActionType.INSPECT) { printResult(facade.performAction(action)); }
        }
    }

    private void runSession() {
        while (!facade.isFinished()) {
            System.out.println("\n" + facade.getStatus());
            List<GameAction> actions = facade.getAvailableActions();
            for (int i = 0; i < actions.size(); i++) {
                System.out.println((i + 1) + ". " + actions.get(i).getLabel());
            }
            int choice = InputReader.readInt(scanner, "Choose an action", 1, actions.size());
            GameAction action = actions.get(choice - 1);
            TurnResult result = facade.performAction(action);
            printResult(result);
            if (action.getType() == ActionType.INSPECT || action.getType() == ActionType.VIEW_STATUS) {
                System.out.println("No turn consumed.");
            }
        }
    }

    private void printResult(TurnResult result) {
        for (String message : result.getMessages()) { System.out.println(message); }
    }
}
