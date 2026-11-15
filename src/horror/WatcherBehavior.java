package horror;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import dimension.DimensionRules.HorrorInteraction;
import engine.ActionType;
import engine.GameAction;

public final class WatcherBehavior implements HorrorBehavior {
    public static final int REQUIRED_ADVANCES = 3;
    static final String CLUE = "The closet door is partly open. The table offers little cover from the doorway.";

    private boolean alert = true;
    private int advances;

    public boolean isAlert() { return alert; }
    public int getAdvances() { return advances; }

    @Override
    public List<GameAction> availableActions(List<HorrorInteraction> interactions) {
        List<GameAction> actions = new ArrayList<>();
        if (alert) { addHidingChoices(actions, interactions); }
        if (interactions.contains(HorrorInteraction.ADVANCE)) {
            actions.add(GameAction.of(ActionType.GO_FORWARD, "Go forward"));
        }
        return actions;
    }

    private void addHidingChoices(List<GameAction> actions, List<HorrorInteraction> interactions) {
        if (interactions.contains(HorrorInteraction.HIDE) && interactions.contains(HorrorInteraction.CHOOSE_HIDING_LOCATION)) {
            actions.add(GameAction.of(ActionType.HIDE_CLOSET, "Hide in the closet"));
            actions.add(GameAction.of(ActionType.HIDE_TABLE, "Hide behind the table"));
        } else if (interactions.contains(HorrorInteraction.WAIT)) {
            actions.add(GameAction.of(ActionType.HIDE_WAIT, "Hide / Wait"));
        }
    }

    @Override
    public EncounterStep resolve(GameAction action, Random random) {
        switch (action.getType()) {
            case HIDE_WAIT:
            case HIDE_CLOSET: return distract("You stay out of sight. The Watcher looks away.");
            case HIDE_TABLE: return EncounterStep.fatal("The Watcher notices you behind the table.");
            default: return advance();
        }
    }

    private EncounterStep distract(String message) {
        alert = false;
        return EncounterStep.progress(message, 1);
    }

    private EncounterStep advance() {
        if (alert) { return EncounterStep.fatal("The Watcher sees you move and attacks."); }
        advances++;
        alert = true;
        String progress = "You creep forward (" + advances + "/" + REQUIRED_ADVANCES + ").";
        if (advances == REQUIRED_ADVANCES) { return EncounterStep.cleared(progress + " The room is clear.", 1); }
        return EncounterStep.progress(progress + " The Watcher turns back toward you.", 1);
    }

    @Override
    public boolean isCleared() { return advances == REQUIRED_ADVANCES; }

    @Override
    public String inspect(List<HorrorInteraction> interactions) {
        String text = "The Watcher is " + (alert ? "ALERT" : "DISTRACTED") + ". Progress " + advances + "/" + REQUIRED_ADVANCES + ".";
        return interactions.contains(HorrorInteraction.CHOOSE_HIDING_LOCATION) ? text + " " + CLUE : text;
    }

    @Override
    public String describeState() { return "Watcher " + (alert ? "ALERT" : "DISTRACTED") + ", progress " + advances + "/" + REQUIRED_ADVANCES; }
}