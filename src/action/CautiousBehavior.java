package action;

public final class CautiousBehavior implements EnemyBehavior {
    static final int DEFENDING_DAMAGE_PERCENT = 70;

    private EnemyStance stance = EnemyStance.DEFENDING;

    @Override
    public EnemyStance currentStance() { return stance; }

    @Override
    public int adjustIncomingDamage(int rawDamage) {
        if (stance == EnemyStance.ATTACKING) { return rawDamage; }
        return (rawDamage * DEFENDING_DAMAGE_PERCENT + 99) / 100;
    }

    @Override
    public int act(int configuredDamage) {
        boolean attacks = stance == EnemyStance.ATTACKING;
        stance = attacks ? EnemyStance.DEFENDING : EnemyStance.ATTACKING;
        return attacks ? configuredDamage : 0;
    }
}