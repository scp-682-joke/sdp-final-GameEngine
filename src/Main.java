import java.util.NoSuchElementException;
import java.util.Scanner;
import console.ConsoleController;
import console.ProjectEditor;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("GAME SCENARIO BUILDER");
            new ConsoleController().runMenu(scanner, ProjectEditor.configureProject(scanner));
        } catch (NoSuchElementException exception) {
            System.out.println("\nInput closed. Goodbye.");
        }
    }
}
