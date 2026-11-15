package facade;

import java.util.List;
import java.util.Random;
import action.ActionEngine;
import config.GameProject;
import creation.ActionScenarioCreator;
import creation.HorrorScenarioCreator;
import creation.ScenarioCreator;
import dimension.ActionScenario;
import dimension.DimensionRules;
import dimension.GameScenario;
import dimension.HorrorScenario;
import dimension.Rules2D;
import dimension.Rules3D;
import engine.GameAction;
import engine.GameEngine;
import engine.GameOutcome;
import engine.TurnResult;
import events.GameEventPublisher;
import events.ResultLogger;
import events.StatisticsTracker;
import horror.HorrorEngine;

public final class GameEngineFacade {
    private static final long GAMEPLAY_SEED_SALT = 0x5DEECE66DL;

    private GameProject project;
    private GameEngine engine;
    private StatisticsTracker statistics;
    private ResultLogger logger;
    private GameEventPublisher publisher;

    public void startSimulation(GameProject project, long seed) {
        DimensionRules rules = "2D".equals(project.getDimension()) ? new Rules2D() : new Rules3D();
        ScenarioCreator creator = "ACTION".equals(project.getGenre())
                ? new ActionScenarioCreator(rules) : new HorrorScenarioCreator(rules);
        GameScenario scenario = creator.create(project, seed);
        GameEventPublisher newPublisher = new GameEventPublisher();
        StatisticsTracker newStatistics = new StatisticsTracker();
        ResultLogger newLogger = new ResultLogger();
        newPublisher.subscribe(newStatistics);
        newPublisher.subscribe(newLogger);
        Random gameplayRandom = new Random(seed ^ GAMEPLAY_SEED_SALT);
        GameEngine newEngine = scenario instanceof ActionScenario
                ? new ActionEngine((ActionScenario) scenario, newPublisher, gameplayRandom)
                : new HorrorEngine((HorrorScenario) scenario, newPublisher, gameplayRandom);
        this.project = project;
        this.publisher = newPublisher;
        this.statistics = newStatistics;
        this.logger = newLogger;
        this.engine = newEngine;
    }

    public void restartSimulation(long seed) {
        requireSession();
        startSimulation(project, seed);
    }

    public GameScenario getScenario() { requireSession(); return engine.getScenario(); }
    public GameProject getProject() { requireSession(); return project; }
    public GameOutcome getOutcome() { requireSession(); return engine.getOutcome(); }
    public boolean isFinished() { return getOutcome() != GameOutcome.IN_PROGRESS; }
    public List<GameAction> getAvailableActions() { requireSession(); return engine.getAvailableActions(); }
    public TurnResult performAction(GameAction action) { requireSession(); return engine.perform(action); }
    public String getStatus() { requireSession(); return engine.getStatus(); }
    public StatisticsTracker getStatistics() { requireSession(); return statistics; }
    public GameEventPublisher getPublisher() { requireSession(); return publisher; }

    public String getResults() {
        requireSession();
        StringBuilder text = new StringBuilder(engine.getResults(statistics));
        text.append("\nEvent log:");
        for (String line : logger.getLines()) { text.append("\n  ").append(line); }
        return text.toString();
    }

    private void requireSession() {
        if (engine == null) { throw new IllegalStateException("No simulation has been started."); }
    }
}
