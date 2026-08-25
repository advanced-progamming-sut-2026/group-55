package pvz.model.entity.plant.category.sun;

import java.util.List;

public interface SunProfile {

    List<Integer> getCycleDrops(long currentTick);

    List<Integer> getPlantFoodDrops(long currentTick);

    default void applyPlantFoodEffect() {
    }

    default SunProductionMode getProductionMode() {
        return SunProductionMode.PERIODIC;
    }

    default boolean activatesImmediatelyAfterPlacement() {
        return getProductionMode() == SunProductionMode.SINGLE_USE_ON_PLACEMENT;
    }

    default boolean removesProducerAfterProduction() {
        return getProductionMode() == SunProductionMode.SINGLE_USE_ON_PLACEMENT;
    }

    default boolean supportsPlantFood() {
        return true;
    }
}
