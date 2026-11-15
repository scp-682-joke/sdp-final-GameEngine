package action;

public interface EnemyBehavior {
    String AGGRESSIVE = "AGGRESSIVE";
    String CAUTIOUS = "CAUTIOUS";

    EnemyStance currentStance();

    int adjustIncomingDamage(int rawDamage);

    int act(int configuredDamage);

    static EnemyBehavior forName(String behaviorName) {
        if (AGGRESSIVE.equals(behaviorName)) { return new AggressiveBehavior(); }
        if (CAUTIOUS.equals(behaviorName)) { return new CautiousBehavior(); }
        throw new IllegalArgumentException("Unknown enemy behavior: " + behaviorName);
    }
}