package creation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import config.ActionConfig;
import config.ActionEnemySpec;
import config.HorrorConfig;
import config.HorrorEnemySpec;
import dimension.ActionScenario.Building;
import dimension.ActionScenario.EnemyInstance;
import dimension.HorrorScenario.EncounterRoom;

public final class ScenarioSpawner {
    private ScenarioSpawner() { }

    static List<Building> spawnBuildings(ActionConfig config, Random random) {
        List<EnemyInstance> enemies = new ArrayList<>();
        for (ActionEnemySpec spec : config.getEnemies()) {
            for (int i = 0; i < spec.getQuantity(); i++) {
                enemies.add(new EnemyInstance(spec));
            }
        }
        Collections.shuffle(enemies, random);
        int split = 1 + random.nextInt(enemies.size() - 1);
        return List.of(new Building(1, enemies.subList(0, split)),
                new Building(2, enemies.subList(split, enemies.size())));
    }

    static List<EncounterRoom> spawnRooms(HorrorConfig config, Random random) {
        List<EncounterRoom> rooms = new ArrayList<>();
        for (HorrorEnemySpec spec : config.getEnemies()) {
            for (int i = 0; i < spec.getQuantity(); i++) {
                rooms.add(new EncounterRoom(spec));
            }
        }
        Collections.shuffle(rooms, random);
        return rooms;
    }
}
