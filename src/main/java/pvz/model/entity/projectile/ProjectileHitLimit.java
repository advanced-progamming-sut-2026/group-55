package pvz.model.entity.projectile;

public record ProjectileHitLimit(int maximumZombieHits) {
    private static final int UNLIMITED_HITS = Integer.MAX_VALUE;

    public ProjectileHitLimit {
        if (maximumZombieHits <= 0) {
            throw new IllegalArgumentException(
                    "maximum zombie hits must be positive"
            );
        }
    }

    public static ProjectileHitLimit singleHit() {
        return limitedTo(1);
    }

    public static ProjectileHitLimit limitedTo(
            int maximumZombieHits
    ) {
        return new ProjectileHitLimit(maximumZombieHits);
    }

    public static ProjectileHitLimit unlimited() {
        return new ProjectileHitLimit(UNLIMITED_HITS);
    }

    public boolean isReachedBy(int zombieHitCount) {
        if (zombieHitCount < 0) {
            throw new IllegalArgumentException(
                    "zombie hit count cannot be negative"
            );
        }

        return zombieHitCount >= maximumZombieHits;
    }

    public boolean isUnlimited() {
        return maximumZombieHits == UNLIMITED_HITS;
    }
}
