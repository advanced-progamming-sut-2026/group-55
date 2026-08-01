package pvz.model.entity.plant.lifecycle;

public enum PlantThreat {

    DAMAGE(true, true),

    PLUCK(true, false),

    EXPIRATION(true, true),

    INSTANT_DESTROY(true, true),

    FORCED_REMOVAL(true, true),

    SYSTEM_CLEANUP(false, false);

    private final boolean blockedByPlantFood;

    private final boolean setsHealthToZeroOnRemoval;

    PlantThreat(
            boolean blockedByPlantFood,
            boolean setsHealthToZeroOnRemoval
    ) {
        this.blockedByPlantFood = blockedByPlantFood;

        this.setsHealthToZeroOnRemoval = setsHealthToZeroOnRemoval;
    }

    public boolean isBlockedByPlantFood() {
        return blockedByPlantFood;
    }

    public boolean setsHealthToZeroOnRemoval() {
        return setsHealthToZeroOnRemoval;
    }
}
