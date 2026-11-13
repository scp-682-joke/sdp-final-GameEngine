package creation;

import java.util.Objects;
import java.util.Random;
import config.GameProject;
import config.ProjectValidator;
import dimension.DimensionRules;
import dimension.GameScenario;

public abstract class ScenarioCreator {
    private final DimensionRules rules;

    protected ScenarioCreator(DimensionRules rules) {
        this.rules = Objects.requireNonNull(rules, "Dimension rules are required.");
    }

    public final GameScenario create(GameProject project, long seed) {
        ProjectValidator.validate(project);
        if (!project.getDimension().equals(rules.getDimension())) {
            throw new IllegalArgumentException("Dimension rules must match the project dimension.");
        }
        return createScenario(project, new Random(seed));
    }

    protected DimensionRules getRules() { return rules; }
    protected abstract GameScenario createScenario(GameProject project, Random random);
}
