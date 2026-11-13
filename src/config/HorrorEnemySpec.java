package config;

public final class HorrorEnemySpec {
    private final String name;
    private final String behavior;
    private final int quantity;

    public HorrorEnemySpec(String name, String behavior, int quantity) {
        this.name = name;
        this.behavior = behavior;
        this.quantity = quantity;
    }

    public String getName() { return name; }
    public String getBehavior() { return behavior; }
    public int getQuantity() { return quantity; }
}
