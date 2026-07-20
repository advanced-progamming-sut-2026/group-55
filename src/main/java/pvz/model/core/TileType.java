package pvz.model.core;

public enum TileType {
    NORMAL(true, 0, false),
    TOMBSTONE(false, 700, true),
    SLIPPERY_UP(false, 0, false),
    SLIPPERY_DOWN(false, 0, false),
    FROZEN(false, 600, true),
    WATER(false, 0, false),
    LOW_BEACH(true, 0, false),
    NECROMANCY(true, 0, false);

    private final boolean normallyPlantable;
    private final double initialHealth;
    private final boolean blocksStraightProjectiles;

    TileType(boolean normallyPlantable, double initialHealth, boolean blocksStraightProjectiles) {
        this.normallyPlantable = normallyPlantable;
        this.initialHealth = initialHealth;
        this.blocksStraightProjectiles = blocksStraightProjectiles;
    }

    public boolean isNormallyPlantable() {
        return normallyPlantable;
    }

    public boolean isDestructible() {
        return initialHealth > 0;
    }

    public double getInitialHealth() {
        return initialHealth;
    }

    public boolean blocksStraightProjectiles() {
        return blocksStraightProjectiles;
    }

    public int getLaneShift() {
        if (this == SLIPPERY_UP) {
            return -1;
        }
        if (this == SLIPPERY_DOWN) {
            return 1;
        }
        return 0;
    }
}