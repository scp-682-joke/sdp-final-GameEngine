package creation;

import java.util.Random;
import config.GameProject;
import dimension.DimensionRules;
import dimension.GameScenario;
import dimension.HorrorScenario;

public final class HorrorScenarioCreator extends ScenarioCreator {
    public HorrorScenarioCreator(DimensionRules rules) { super(rules); }

    @Override
    protected GameScenario createScenario(GameProject project, Random random) {
        if (!"HORROR".equals(project.getGenre())) {
            throw new IllegalArgumentException("Horror creator requires a HORROR project.");
        }
        return new HorrorScenario(project, getRules(), ScenarioSpawner.spawnRooms(project.getHorrorConfig(), random));
    }
}
