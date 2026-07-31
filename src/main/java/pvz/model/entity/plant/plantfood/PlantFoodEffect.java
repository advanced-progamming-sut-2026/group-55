package pvz.model.entity.plant.plantfood;

import pvz.model.entity.plant.Plant;

@FunctionalInterface
public interface PlantFoodEffect {

    void apply(
            Plant plant,
            long currentTick,
            long durationTicks
    );
}
