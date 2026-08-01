package pvz.model.entity.plant.plantfood;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.plant.lifecycle.PlantLifecycle;

public final class PlantFoodController {

    private final String plantName;

    private final PlantLifecycle lifecycle;

    private final PlantFoodCapability capability;

    private final BooleanSupplier ownerPresentInWorld;

    private final PlantFoodPreparation preparation;

    public PlantFoodController(
            String plantName,
            PlantLifecycle lifecycle,
            PlantFoodCapability capability,
            BooleanSupplier ownerPresentInWorld,
            PlantFoodPreparation preparation
    ) {
        this.plantName = Objects.requireNonNull(
                        plantName,
                        "plant name cannot be null"
                ).strip();

        if (this.plantName.isEmpty()) {
            throw new IllegalArgumentException(
                    "plant name cannot be blank"
            );
        }

        this.lifecycle =
                Objects.requireNonNull(
                        lifecycle,
                        "plant lifecycle cannot be null"
                );

        this.capability = capability;

        this.ownerPresentInWorld = Objects.requireNonNull(
                        ownerPresentInWorld,
                        "owner presence check cannot be null"
                );

        this.preparation = Objects.requireNonNull(
                        preparation,
                        "plant food preparation cannot be null"
                );
    }

    public boolean supportsPlantFood() {
        return capability != null;
    }

    public boolean tryActivate(long currentTick) {
        PlantFoodCapability supportedCapability = requireCapability();

        if (!ownerPresentInWorld.getAsBoolean()) {
            throw new IllegalStateException(
                    plantName + " must be present in the world before plant food can be applied"
            );
        }

        long durationTicks = PlantFoodRules
                        .effectiveDurationTicks(
                                supportedCapability
                                        .requestedDurationTicks()
                        );

        boolean activated = lifecycle.tryActivatePlantFood(currentTick, durationTicks);

        if (!activated) {
            return false;
        }

        preparation.prepare(currentTick, durationTicks);

        supportedCapability.onPlantFoodStarted(currentTick, durationTicks);

        supportedCapability.applyPlantFood(currentTick, durationTicks);

        return true;
    }

    public boolean isActive(long currentTick) {
        return lifecycle.isPlantFoodActive(currentTick);
    }

    public long getRemainingTicks(long currentTick) {
        return lifecycle.getRemainingPlantFoodTicks(currentTick);
    }

    private PlantFoodCapability requireCapability() {
        if (capability == null) {
            throw new IllegalStateException(
                    plantName + " does not support plant food"
            );
        }

        return capability;
    }
}
