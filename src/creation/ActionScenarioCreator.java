package creation;

import java.util.Random;
import config.GameProject;
import dimension.ActionScenario;
import dimension.DimensionRules;
import dimension.GameScenario;

public final class ActionScenarioCreator extends ScenarioCreator {
    public ActionScenarioCreator(DimensionRules rules) { super(rules); }

    @Override
    protected GameScenario createScenario(GameProject project, Random random) {
        if (!"ACTION".equals(project.getGenre())) {
            throw new IllegalArgumentException("Action creator requires an ACTION project.");
        }
        return new ActionScenario(project, getRules(), ScenarioSpawner.spawnBuildings(project.getActionConfig(), random));
    }
}
