package dimension;

import java.util.List;

public interface DimensionRules {
    enum ActionInteraction { NORMAL_ATTACK, MOVING_EVASIVE_ATTACK }
    enum HorrorInteraction { WAIT, HIDE, ADVANCE, SEARCH_OBJECT, DISTRACT, CHOOSE_HIDING_LOCATION }

    String getDimension();
    List<ActionInteraction> availableActionInteractions();
    List<HorrorInteraction> availableHorrorInteractions();
}
