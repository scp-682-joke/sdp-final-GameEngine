package horror;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import dimension.DimensionRules.HorrorInteraction;
import engine.GameAction;
import engine.ActionType;

public final class DistractorBehavior implements HorrorBehavior {
    public static final int SEARCH_SUCCESS_PERCENT = 50;
    public static final int GUARANTEED_SEARCH_ATTEMPT = 3;
    static final int DIRECT_ROUTE_COST = 1;
    static final int SIDE_ROUTE_COST = 2;

    private int searchAttempts;
    private boolean itemFound;
    private boolean distracted;
    private boolean cleared;

    public int getSearchAttempts() { return searchAttempts; }
    public boolean isItemFound() { return itemFound; }
    public boolean isDistracted() { return distracted; }

    @Override
    public List<GameAction> availableActions(List<HorrorInteraction> interactions) {
        List<GameAction> actions = new ArrayList<>();
        if (!itemFound && interactions.contains(HorrorInteraction.SEARCH_OBJECT)) {
            actions.add(GameAction.of(ActionType.SEARCH, "Search the room"));
        }
        if (itemFound && !distracted && interactions.contains(HorrorInteraction.DISTRACT)) {
            actions.add(GameAction.of(ActionType.USE_ITEM, "Throw the metal can to distract the enemy"));
        }
        if (interactions.contains(HorrorInteraction.ADVANCE)) {
            actions.add(GameAction.of(ActionType.GO_FORWARD, "Go forward"));
            if (interactions.contains(HorrorInteraction.CHOOSE_HIDING_LOCATION)) {
                actions.add(GameAction.of(ActionType.GO_FORWARD_SIDE_ROUTE, "Take the side route (costs one extra turn)"));
            }
        }
        return actions;
    }

    @Override
    public EncounterStep resolve(GameAction action, Random random) {
        switch (action.getType()) {
            case SEARCH: return search(random);
            case USE_ITEM: return useItem();
            case GO_FORWARD_SIDE_ROUTE: return pass(SIDE_ROUTE_COST);
            default: return pass(DIRECT_ROUTE_COST);
        }
    }

    private EncounterStep search(Random random) {
        searchAttempts++;
        itemFound = searchAttempts >= GUARANTEED_SEARCH_ATTEMPT || random.nextInt(100) < SEARCH_SUCCESS_PERCENT;
        return EncounterStep.progress(itemFound ? "You find a metal can." : "You search but find nothing useful.", 1);
    }

    private EncounterStep useItem() {
        distracted = true;
        return EncounterStep.progress("You throw the can. The enemy rushes toward the noise.", 1);
    }

    private EncounterStep pass(int timeCost) {
        if (!distracted) { return EncounterStep.fatal("The enemy catches you trying to pass."); }
        cleared = true;
        return EncounterStep.cleared("You slip past the distracted enemy. The room is clear.", timeCost);
    }

    @Override
    public boolean isCleared() { return cleared; }

    @Override
    public String inspect(List<HorrorInteraction> interactions) {
        return "A guard blocks the passage. " + describeState() + ".";
    }

    @Override
    public String describeState() {
        return "Distractor " + (distracted ? "DISTRACTED" : "BLOCKING") + ", item " + (itemFound ? "found" : "not found")
                + ", searches " + searchAttempts;
    }
}