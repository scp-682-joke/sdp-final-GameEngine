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
    private final GameEngineFacade facade = new GameEngineFacade();

    GameConsole(Scanner scanner, Random seeds) {
        this.scanner = scanner;
        this.seeds = seeds;
    }

    void play(GameProject project) {
        do {
            if (!startSession(project)) { return; }
            runSession();
            System.out.println("\n=== RESULTS ===\n" + facade.getResults());
        } while (Main.readInt(scanner, "\n1. Generate a new scenario and play again\n2. Back to project menu", 1, 2) == 1);
    }

    private boolean startSession(GameProject project) {
        long seed = seeds.nextLong();
        try {
            facade.startSimulation(project, seed);
        } catch (IllegalArgumentException exception) {
            System.out.println("Cannot start simulation: " + exception.getMessage());
            return false;
        }
        System.out.println("\n=== SIMULATION STARTED (seed " + seed + ") ===");
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
            int choice = Main.readInt(scanner, "Choose an action", 1, actions.size());
            printResult(facade.performAction(actions.get(choice - 1)));
        }
    }

    private void printResult(TurnResult result) {
        for (String message : result.getMessages()) { System.out.println(message); }
    }
}