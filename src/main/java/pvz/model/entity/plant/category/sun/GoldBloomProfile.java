package pvz.model.entity.plant.category.sun;

import java.util.Collections;
import java.util.List;

import pvz.model.entity.collectible.sun.SunValue;

public final class GoldBloomProfile implements SunProfile {

    public static final int SUN_VALUE = SunValue.BIGSUN.getValue();

    public static final int SUN_COUNT = 5;

    public static final int TOTAL_SUN = SUN_VALUE * SUN_COUNT;

    @Override
    public List<Integer> getCycleDrops(long currentTick) {
        return Collections.nCopies(SUN_COUNT, SUN_VALUE);
    }

    @Override
    public List<Integer> getPlantFoodDrops(long currentTick) {
        return List.of();
    }

    @Override
    public SunProductionMode getProductionMode() {
        return SunProductionMode.SINGLE_USE_ON_PLACEMENT;
    }

    @Override
    public boolean supportsPlantFood() {
        return false;
    }
}
