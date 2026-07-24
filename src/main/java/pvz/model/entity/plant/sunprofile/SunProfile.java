package pvz.model.entity.plant.sunprofile;

import java.util.List;

public interface SunProfile {

    List<Integer> getCycleDrops(long currentTick);

    List<Integer> getPlantFoodDrops(long currentTick);

    default void applyPlantFoodEffect() {
    }
}