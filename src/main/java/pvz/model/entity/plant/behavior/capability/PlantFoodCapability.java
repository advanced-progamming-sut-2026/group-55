package pvz.model.entity.plant.behavior.capability;

import pvz.model.entity.plant.plantfood.PlantFoodRules;

public interface PlantFoodCapability {

    boolean supportsPlantFood();

    default long requestedDurationTicks() {
        return PlantFoodRules.MINIMUM_DURATION_TICKS;
    }

    default boolean canReceivePlantFood(long currentTick) {
        return true;
    }

    default boolean targetsMatchingPlantsOnBoard() {
        return false;
    }

    default void onPlantFoodStarted(long currentTick, long durationTicks) {
    }

    void applyPlantFood(long currentTick, long durationTicks);
}
