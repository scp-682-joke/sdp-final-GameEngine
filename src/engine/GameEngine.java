package engine;

import java.util.List;
import dimension.GameScenario;
import events.StatisticsTracker;

public interface GameEngine {
    GameScenario getScenario();
    GameOutcome getOutcome();
    List<GameAction> getAvailableActions();
    TurnResult perform(GameAction action);
    String getStatus();
    String getResults(StatisticsTracker statistics);
}