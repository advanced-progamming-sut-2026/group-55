package pvz.model.entity.zombie;

import java.util.Objects;

public record ZombieRuntimeStats(
        double maxHealth,
        double eatDamagePerSecond,
        double tilesPerSecond,
        int waveCost
) {
    public ZombieRuntimeStats {
        if (maxHealth <= 0
                || eatDamagePerSecond < 0
                || tilesPerSecond < 0
                || waveCost <= 0) {
            throw new IllegalArgumentException(
                    "invalid zombie runtime stats"
            );
        }
    }

    public static ZombieRuntimeStats from(
            ZombieSpec spec,
            int difficultyLevel
    ) {
        Objects.requireNonNull(spec, "zombie spec cannot be null");
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            throw new IllegalArgumentException(
                    "difficulty level must be between 1 and 5"
            );
        }
        double increaseMultiplier = difficultyLevel / 3.0;
        double decreaseMultiplier = 3.0 / difficultyLevel;
        return new ZombieRuntimeStats(
                spec.getHitpoints() * increaseMultiplier,
                spec.getEatDps() * increaseMultiplier,
                spec.getSpeed(),
                Math.max(
                        1,
                        (int) Math.round(
                                spec.getWaveCost() * decreaseMultiplier
                        )
                )
        );
    }
}
