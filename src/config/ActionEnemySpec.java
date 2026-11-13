package config;

public final class ActionEnemySpec {
    private final String name;
    private final int maxHealth;
    private final int damage;
    private final String behavior;
    private final int quantity;

    public ActionEnemySpec(String name, int maxHealth, int damage, String behavior, int quantity) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.behavior = behavior;
        this.quantity = quantity;
    }

    public String getName() { return name; }
    public int getMaxHealth() { return maxHealth; }
    public int getDamage() { return damage; }
    public String getBehavior() { return behavior; }
    public int getQuantity() { return quantity; }
}
