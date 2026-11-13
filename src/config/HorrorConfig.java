package config;

import java.util.List;

public final class HorrorConfig {
    private final List<HorrorEnemySpec> enemies;
    private final String pursuerName;
    private final int pursuerCountdown;

    HorrorConfig(List<HorrorEnemySpec> enemies, String pursuerName, int pursuerCountdown) {
        this.enemies = List.copyOf(enemies);
        this.pursuerName = pursuerName;
        this.pursuerCountdown = pursuerCountdown;
    }

    public List<HorrorEnemySpec> getEnemies() { return enemies; }
    public String getPursuerName() { return pursuerName; }
    public int getPursuerCountdown() { return pursuerCountdown; }
}
