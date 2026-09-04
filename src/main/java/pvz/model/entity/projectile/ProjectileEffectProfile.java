package pvz.model.entity.projectile;

import pvz.model.core.Game;

public record ProjectileEffectProfile(
        long chillDurationTicks,
        long poisonDurationTicks,
        double poisonDamagePerSecond,
        int maximumPoisonStacks
) {
    public static final ProjectileEffectProfile DEFAULT = new ProjectileEffectProfile(
            10L * Game.TICKS_PER_SECOND,
            8L * Game.TICKS_PER_SECOND,
            3,
            5
    );

    public ProjectileEffectProfile {
        if (chillDurationTicks < 0 || poisonDurationTicks < 0) {
            throw new IllegalArgumentException("effect duration cannot be negative");
        }
        if (!Double.isFinite(poisonDamagePerSecond) || poisonDamagePerSecond < 0) {
            throw new IllegalArgumentException("poison damage must be finite and non-negative");
        }
        if (maximumPoisonStacks < 1) {
            throw new IllegalArgumentException("maximum poison stacks must be positive");
        }
    }
}
