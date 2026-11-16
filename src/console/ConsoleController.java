package console;

import java.util.Random;
import java.util.Scanner;
import config.GameProject;
import facade.GameEngineFacade;
import static console.InputReader.*;
import static console.ProjectPrinter.*;

public final class ConsoleController {
    private final GameEngineFacade facade = new GameEngineFacade();

    public void runMenu(Scanner scanner, GameProject project) {
        Random seeds = new Random();
        GameConsole console = new GameConsole(scanner, seeds, facade);
        Long generationSeed = null;
        printProject(project);
        System.out.println("Project validation: SUCCESS");
        while (true) {
            int choice = readInt(scanner, "\n1. View project configuration\n2. Generate randomized scenario\n3. Generate again"
                    + "\n4. Start simulation\n5. Create a new project\n6. Exit", 1, 6);
            if (choice == 6) { return; }
            if (choice == 1) {
                printProject(project);
            } else if (choice == 4) {
                if (console.play(project, generationSeed)) { return; }
            } else if (choice == 5) {
                project = ProjectEditor.configureProject(scanner);
                generationSeed = null;
                printProject(project);
            } else {
                generationSeed = seeds.nextLong();
                System.out.println("Generation seed: " + generationSeed);
                printScenario(facade.generateScenario(project, generationSeed));
            }
        }
    }

}

