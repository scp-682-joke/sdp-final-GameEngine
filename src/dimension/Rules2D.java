package dimension;

import java.util.List;

public final class Rules2D implements DimensionRules {
    public String getDimension() { return "2D"; }
    public List<ActionInteraction> availableActionInteractions() {
        return List.of(ActionInteraction.NORMAL_ATTACK);
    }
    public List<HorrorInteraction> availableHorrorInteractions() {
        return List.of(HorrorInteraction.WAIT, HorrorInteraction.HIDE, HorrorInteraction.ADVANCE,
                HorrorInteraction.SEARCH_OBJECT, HorrorInteraction.DISTRACT);
    }
}
