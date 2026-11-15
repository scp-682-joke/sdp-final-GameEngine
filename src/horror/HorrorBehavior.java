package horror;

import java.util.List;
import java.util.Random;
import dimension.DimensionRules.HorrorInteraction;
import engine.GameAction;

public interface HorrorBehavior {
    String WATCHER = "WATCHER";
    String DISTRACTOR = "DISTRACTOR";

    List<GameAction> availableActions(List<HorrorInteraction> interactions);

    EncounterStep resolve(GameAction action, Random random);

    boolean isCleared();

    String inspect(List<HorrorInteraction> interactions);

    String describeState();

    static HorrorBehavior forName(String behaviorName) {
        if (WATCHER.equals(behaviorName)) { return new WatcherBehavior(); }
        if (DISTRACTOR.equals(behaviorName)) { return new DistractorBehavior(); }
        throw new IllegalArgumentException("Unknown horror behavior: " + behaviorName);
    }
}