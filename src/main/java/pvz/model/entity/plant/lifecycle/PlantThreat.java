package pvz.model.entity.plant.lifecycle;

public enum PlantThreat {

    DAMAGE(true, true),

    PLUCK(true, false),

    EXPIRATION(true, true),

    INSTANT_DESTROY(true, true),

    FORCED_REMOVAL(true, true),

    SUPPORT_LOSS(false, true),

    FREEZE(true, false),

    OCTOPUS(true, false),

    ACTION_BLOCK(true, false),

    FORCED_RELOCATION(true, false),

    TRANSIENT_EFFECT_COMPLETION(false, true),

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
