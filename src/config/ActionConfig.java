package config;

import java.util.List;

public final class ActionConfig {
    private final int playerHealth;
    private final List<ActionEnemySpec> enemies;

    ActionConfig(int playerHealth, List<ActionEnemySpec> enemies) {
        this.playerHealth = playerHealth;
        this.enemies = List.copyOf(enemies);
    }

    public int getPlayerHealth() { return playerHealth; }
    public List<ActionEnemySpec> getEnemies() { return enemies; }
}
