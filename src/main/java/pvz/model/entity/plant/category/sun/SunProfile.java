package pvz.model.entity.plant.category.sun;

import java.util.List;

public interface SunProfile {

    List<Integer> getCycleDrops(long currentTick);

    List<Integer> getPlantFoodDrops(long currentTick);

    default void applyPlantFoodEffect() {
    }
}
