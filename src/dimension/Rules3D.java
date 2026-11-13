package dimension;

import java.util.List;

public final class Rules3D implements DimensionRules {
    public String getDimension() { return "3D"; }
    public List<ActionInteraction> availableActionInteractions() {
        return List.of(ActionInteraction.NORMAL_ATTACK, ActionInteraction.MOVING_EVASIVE_ATTACK);
    }
    public List<HorrorInteraction> availableHorrorInteractions() {
        return List.of(HorrorInteraction.WAIT, HorrorInteraction.HIDE, HorrorInteraction.ADVANCE,
                HorrorInteraction.SEARCH_OBJECT, HorrorInteraction.DISTRACT, HorrorInteraction.CHOOSE_HIDING_LOCATION);
    }
}
