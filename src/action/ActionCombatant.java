package action;

import dimension.ActionScenario.EnemyInstance;

public final class ActionCombatant {
    private final String label;
    private final EnemyInstance enemy;
    private final EnemyBehavior behavior;

    public ActionCombatant(String label, EnemyInstance enemy) {
        this.label = label;
        this.enemy = enemy;
        this.behavior = EnemyBehavior.forName(enemy.getSpecification().getBehavior());
    }

    public String getLabel() { return label; }
    public EnemyInstance getEnemy() { return enemy; }
    public EnemyBehavior getBehavior() { return behavior; }
    public boolean isAlive() { return enemy.getCurrentHealth() > 0; }

    public int receiveHit(int rawDamage) {
        int taken = Math.min(behavior.adjustIncomingDamage(rawDamage), enemy.getCurrentHealth());
        enemy.setCurrentHealth(enemy.getCurrentHealth() - taken);
        return taken;
    }

    public int act() {
        return isAlive() ? behavior.act(enemy.getSpecification().getDamage()) : 0;
    }

    public String describe() {
        return label + " | HP " + enemy.getCurrentHealth() + "/" + enemy.getSpecification().getMaxHealth();
    }
}
