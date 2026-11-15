package events;

public final class StatisticsTracker implements GameEventListener {
    private int enemiesDefeated;
    private int buildingsCleared;
    private int roomsCleared;

    @Override
    public void onEvent(GameEvent event) {
        switch (event.getType()) {
            case ENEMY_DEFEATED: enemiesDefeated++; break;
            case BUILDING_CLEARED: buildingsCleared++; break;
            case ROOM_CLEARED: roomsCleared++; break;
            default: break;
        }
    }

    public int getEnemiesDefeated() { return enemiesDefeated; }
    public int getBuildingsCleared() { return buildingsCleared; }
    public int getRoomsCleared() { return roomsCleared; }
}