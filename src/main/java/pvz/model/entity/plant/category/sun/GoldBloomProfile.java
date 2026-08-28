package pvz.model.entity.plant.category.sun;

import java.util.ArrayList;
import java.util.List;
import pvz.model.entity.collectible.sun.SunValue;

public final class GoldBloomProfile implements SunProfile {
    public static final int BASE_TOTAL_SUN = SunValue.BIGSUN.getValue() * 5;

    private final int totalSun;

    public GoldBloomProfile() {
        this(0);
    }

    public GoldBloomProfile(int bonusSun) {
        if (bonusSun < 0) {
            throw new IllegalArgumentException("Gold Bloom bonus sun cannot be negative");
        }
        totalSun = BASE_TOTAL_SUN + bonusSun;
    }

    @Override
    public List<Integer> getCycleDrops(long currentTick) {
        return splitIntoSupportedSunValues(totalSun);
    }

    private List<Integer> splitIntoSupportedSunValues(int total) {
        List<Integer> result = new ArrayList<>();
        int remaining = total;
        int[] values = {
                SunValue.BIGSUN.getValue(),
                SunValue.NORMALSUN.getValue(),
                SunValue.SMALLSUN.getValue()
        };
        for (int value : values) {
            while (remaining >= value) {
                result.add(value);
                remaining -= value;
            }
        }
        if (remaining != 0) {
            throw new IllegalStateException("Gold Bloom total cannot be represented by supported sun values");
        }
        return List.copyOf(result);
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
