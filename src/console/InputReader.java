package console;

import java.util.Scanner;

public final class InputReader {
    private InputReader() { }

    public static String readText(Scanner scanner, String prompt) {
        System.out.println(prompt);
        System.out.print("> ");
        return scanner.nextLine();
    }

    public static int readInt(Scanner scanner, String prompt, int minimum, int maximum) {
        while (true) {
            String text = readText(scanner, prompt + " [" + minimum + "–" + maximum + "]");
            try {
                int value = Integer.parseInt(text.trim());
                if (value >= minimum && value <= maximum) { return value; }
            } catch (NumberFormatException exception) {
                System.out.println("Invalid input. Please enter an integer.");
                continue;
            }
            System.out.println("Enter a whole number from " + minimum + " to " + maximum + ".");
        }
    }

    public static String readChoice(Scanner scanner, String prompt, String first, String second) {
        while (true) {
            String value = readText(scanner, prompt + "\n1. " + first + "\n2. " + second).trim();
            if ("1".equals(value)) { return first; }
            if ("2".equals(value)) { return second; }
            if (first.equals(value) || second.equals(value)) { return value; }
            System.out.println("Enter " + first + " or " + second + ".");
        }
    }
}
