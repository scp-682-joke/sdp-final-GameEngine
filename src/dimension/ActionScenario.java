package dimension;

import java.util.List;
import config.ActionEnemySpec;
import config.GameProject;

public final class ActionScenario extends GameScenario {
    private final List<Building> buildings;
    private int playerHealth;

    public ActionScenario(GameProject project, DimensionRules rules, List<Building> buildings) {
        super(project, rules);
        this.buildings = List.copyOf(buildings);
        this.playerHealth = project.getActionConfig().getPlayerHealth();
    }

    public List<Building> getBuildings() { return buildings; }
    public int getPlayerHealth() { return playerHealth; }
    public void setPlayerHealth(int health) {
        if (health < 0 || health > getProject().getActionConfig().getPlayerHealth()) {
            throw new IllegalArgumentException("Player HP must be between zero and configured maximum.");
        }
        playerHealth = health;
    }
    public List<DimensionRules.ActionInteraction> getAvailableInteractions() {
        return getDimensionRules().availableActionInteractions();
    }

    public static final class Building {
        private final int number;
        private final List<EnemyInstance> enemies;

        public Building(int number, List<EnemyInstance> enemies) {
            this.number = number;
            this.enemies = List.copyOf(enemies);
        }
        public int getNumber() { return number; }
        public List<EnemyInstance> getEnemies() { return enemies; }
    }

    public static final class EnemyInstance {
        private final ActionEnemySpec specification;
        private int currentHealth;

        public EnemyInstance(ActionEnemySpec specification) {
            this.specification = specification;
            this.currentHealth = specification.getMaxHealth();
        }
        public ActionEnemySpec getSpecification() { return specification; }
        public int getCurrentHealth() { return currentHealth; }
        public void setCurrentHealth(int health) {
            if (health < 0 || health > specification.getMaxHealth()) {
                throw new IllegalArgumentException("Enemy HP must be between zero and configured maximum.");
            }
            currentHealth = health;
        }
    }
}
