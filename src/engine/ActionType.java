package engine;

public enum ActionType {
    ATTACK(false), MOVING_ATTACK(false), ENTER_BUILDING(false), USE_ELEVATOR(false),
    HIDE_WAIT(false), HIDE_CLOSET(false), HIDE_TABLE(false), GO_FORWARD(false),
    GO_FORWARD_SIDE_ROUTE(false), SEARCH(false), USE_ITEM(false), FINAL_ESCAPE(false),
    INSPECT(true), VIEW_STATUS(true);

    private final boolean free;

    ActionType(boolean free) { this.free = free; }

    public boolean isFree() { return free; }
}