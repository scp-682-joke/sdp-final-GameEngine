package config;

import java.util.HashSet;
import java.util.Set;

public final class ProjectValidator {
    private ProjectValidator() { }

    public static void validate(GameProject project) {
        require(project != null, "Project is required.");
        requireName(project.getName());
        require("2D".equals(project.getDimension()) || "3D".equals(project.getDimension()), "Dimension must be 2D or 3D.");
        if ("ACTION".equals(project.getGenre())) {
            validateAction(project);
        } else if ("HORROR".equals(project.getGenre())) {
            validateHorror(project);
        } else {
            throw new IllegalArgumentException("Genre must be ACTION or HORROR.");
        }
    }

    private static void validateAction(GameProject project) {
        ActionConfig config = project.getActionConfig();
        require(project.getHorrorConfig().getEnemies().isEmpty()
                && project.getHorrorConfig().getPursuerName() == null
                && project.getHorrorConfig().getPursuerCountdown() == 0, "Action cannot contain Horror configuration.");
        require(config.getPlayerHealth() >= 1 && config.getPlayerHealth() <= 200, "Player HP must be 1–200.");
        require(config.getEnemies().size() >= 1 && config.getEnemies().size() <= 5, "Action needs 1–5 enemy types.");
        Set<String> names = new HashSet<>();
        long total = 0;
        for (ActionEnemySpec enemy : config.getEnemies()) {
            uniqueName(names, enemy.getName());
            require(enemy.getMaxHealth() >= 1 && enemy.getMaxHealth() <= 200, "Enemy HP must be 1–200.");
            require(enemy.getDamage() >= 1 && enemy.getDamage() <= 20, "Enemy damage must be 1–20.");
            require(enemy.getQuantity() > 0, "Enemy quantity must be positive.");
            require("AGGRESSIVE".equals(enemy.getBehavior()) || "CAUTIOUS".equals(enemy.getBehavior()), "Action behavior must be AGGRESSIVE or CAUTIOUS.");
            total += enemy.getQuantity();
        }
        require(total == 5, "Action requires exactly five enemy instances.");
    }

    private static void validateHorror(GameProject project) {
        require(project.getActionConfig().getEnemies().isEmpty() && project.getActionConfig().getPlayerHealth() == 0,
                "Horror cannot contain Action configuration.");
        HorrorConfig config = project.getHorrorConfig();
        Set<String> names = new HashSet<>();
        uniqueName(names, config.getPursuerName());
        require(config.getPursuerCountdown() > 0, "Pursuer countdown must be positive.");
        boolean watcher = false;
        boolean distractor = false;
        long total = 1;
        for (HorrorEnemySpec enemy : config.getEnemies()) {
            uniqueName(names, enemy.getName());
            require(enemy.getQuantity() > 0, "Enemy quantity must be positive.");
            require("WATCHER".equals(enemy.getBehavior()) || "DISTRACTOR".equals(enemy.getBehavior()), "Horror behavior must be WATCHER or DISTRACTOR.");
            watcher |= "WATCHER".equals(enemy.getBehavior());
            distractor |= "DISTRACTOR".equals(enemy.getBehavior());
            total += enemy.getQuantity();
        }
        require(watcher && distractor, "Horror requires at least one Watcher and one Distractor.");
        require(total >= 3 && total <= 5, "Horror requires 3–5 enemies including one Pursuer.");
    }

    private static void uniqueName(Set<String> names, String name) {
        requireName(name);
        require(names.add(name), "Enemy names must be unique: " + name);
    }

    private static void requireName(String name) {
        require(name != null && !name.isBlank(), "Project and enemy names must not be blank (including Pursuer).");
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new IllegalArgumentException(message); }
    }
}
