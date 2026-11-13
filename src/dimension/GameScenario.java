package dimension;

import java.util.Objects;
import config.GameProject;

public abstract class GameScenario {
    private final GameProject project;
    private final DimensionRules dimensionRules;

    protected GameScenario(GameProject project, DimensionRules dimensionRules) {
        this.project = Objects.requireNonNull(project, "Project is required.");
        this.dimensionRules = Objects.requireNonNull(dimensionRules, "Dimension rules are required.");
        if (!project.getDimension().equals(dimensionRules.getDimension())) {
            throw new IllegalArgumentException("Dimension rules must match the project dimension.");
        }
    }

    public GameProject getProject() { return project; }
    protected DimensionRules getDimensionRules() { return dimensionRules; }
}
