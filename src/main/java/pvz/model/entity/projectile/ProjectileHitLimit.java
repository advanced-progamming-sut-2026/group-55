package pvz.model.entity.projectile;

public record ProjectileHitLimit(int maximumHits) {
    private static final int UNLIMITED_HITS = Integer.MAX_VALUE;

    public ProjectileHitLimit {
        if (maximumHits <= 0) {
            throw new IllegalArgumentException(
                    "maximum hits must be positive"
            );
        }
    }

    public static ProjectileHitLimit singleHit() {
        return limitedTo(1);
    }

    public static ProjectileHitLimit limitedTo(
            int maximumHits
    ) {
        return new ProjectileHitLimit(maximumHits);
    }

    public static ProjectileHitLimit unlimited() {
        return new ProjectileHitLimit(UNLIMITED_HITS);
    }

    public boolean isReachedBy(int hitCount) {
        if (hitCount < 0) {
            throw new IllegalArgumentException(
                    "hit count cannot be negative"
            );
        }

        return hitCount >= maximumHits;
    }

    public boolean isUnlimited() {
        return maximumHits == UNLIMITED_HITS;
    }
}
