package config;

import java.util.ArrayList;
import java.util.List;

public final class GameProjectBuilder {
    private String name;
    private String genre;
    private String dimension;
    private int playerHealth;
    private final List<ActionEnemySpec> actionEnemies = new ArrayList<>();
    private final List<HorrorEnemySpec> horrorEnemies = new ArrayList<>();
    private String pursuerName;
    private int pursuerCountdown;

    public GameProjectBuilder setName(String value) { name = value; return this; }
    public GameProjectBuilder setGenre(String value) { genre = value; return this; }
    public GameProjectBuilder setDimension(String value) { dimension = value; return this; }
    public GameProjectBuilder setPlayerHealth(int value) { playerHealth = value; return this; }

    public GameProjectBuilder addActionEnemy(String name, int health, int damage, String behavior, int quantity) {
        actionEnemies.add(new ActionEnemySpec(name, health, damage, behavior, quantity));
        return this;
    }

    public GameProjectBuilder addHorrorEnemy(String name, String behavior, int quantity) {
        horrorEnemies.add(new HorrorEnemySpec(name, behavior, quantity));
        return this;
    }

    // There is one Pursuer slot; calling this again replaces its configuration.
    public GameProjectBuilder setPursuer(String name, int countdown) {
        pursuerName = name;
        pursuerCountdown = countdown;
        return this;
    }

    public GameProject build() {
        GameProject project = new GameProject(name, genre, dimension,
                new ActionConfig(playerHealth, actionEnemies),
                new HorrorConfig(horrorEnemies, pursuerName, pursuerCountdown));
        ProjectValidator.validate(project);
        return project;
    }
}

