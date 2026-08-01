package pvz.model.entity.plant.plantfood;

@FunctionalInterface
public interface PlantFoodPreparation {

    void prepare(long currentTick, long durationTicks);
}
