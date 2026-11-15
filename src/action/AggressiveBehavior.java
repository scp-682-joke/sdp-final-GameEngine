package action;

public final class AggressiveBehavior implements EnemyBehavior {
    @Override
    public EnemyStance currentStance() { return EnemyStance.ATTACKING; }

    @Override
    public int adjustIncomingDamage(int rawDamage) { return rawDamage; }

    @Override
    public int act(int configuredDamage) { return configuredDamage; }
}