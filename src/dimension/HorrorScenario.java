package dimension;

import java.util.List;
import config.GameProject;
import config.HorrorEnemySpec;

public final class HorrorScenario extends GameScenario {
    private final List<EncounterRoom> rooms;
    private final String pursuerName;
    private int remainingCountdown;

    public HorrorScenario(GameProject project, DimensionRules rules, List<EncounterRoom> rooms) {
        super(project, rules);
        this.rooms = List.copyOf(rooms);
        pursuerName = project.getHorrorConfig().getPursuerName();
        remainingCountdown = project.getHorrorConfig().getPursuerCountdown();
    }

    public List<EncounterRoom> getRooms() { return rooms; }
    public String getPursuerName() { return pursuerName; }
    public int getRemainingCountdown() { return remainingCountdown; }
    public void setRemainingCountdown(int countdown) {
        if (countdown < 0 || countdown > getProject().getHorrorConfig().getPursuerCountdown()) {
            throw new IllegalArgumentException("Countdown must be between zero and configured maximum.");
        }
        remainingCountdown = countdown;
    }
    public List<DimensionRules.HorrorInteraction> getAvailableInteractions() {
        return getDimensionRules().availableHorrorInteractions();
    }

    public static final class EncounterRoom {
        private final HorrorEnemySpec specification;

        public EncounterRoom(HorrorEnemySpec specification) { this.specification = specification; }
        public HorrorEnemySpec getSpecification() { return specification; }
    }
}
