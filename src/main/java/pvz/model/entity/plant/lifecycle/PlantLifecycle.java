package pvz.model.entity.plant.lifecycle;

import java.util.Objects;

import pvz.model.entity.plant.plantfood.PlantFoodState;

public final class PlantLifecycle {
    private final PlantFoodState plantFoodState = new PlantFoodState();

    public boolean tryActivatePlantFood(long currentTick, long durationTicks) {
        return plantFoodState.tryActivate(currentTick, durationTicks);
    }

    public boolean isPlantFoodActive(long currentTick) {
        return plantFoodState.isActive(currentTick);
    }

    public long getRemainingPlantFoodTicks(long currentTick) {
        return plantFoodState.getRemainingTicks(currentTick);
    }

    public boolean allows(PlantThreat threat, long currentTick) {
        Objects.requireNonNull(threat, "plant threat cannot be null");

        if (!plantFoodState.isActive(currentTick)) {
            return true;
        }

        return !threat.isBlockedByPlantFood();
    }
}
