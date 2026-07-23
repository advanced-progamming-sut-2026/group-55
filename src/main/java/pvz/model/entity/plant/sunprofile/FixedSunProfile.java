package pvz.model.entity.plant.sunprofile;

import java.util.Collections;
import java.util.List;

public final class FixedSunProfile implements SunProfile {
    private final int valuePerSun;
    private final int sunsPerCycle;
    private final int plantFoodTotal;

    public FixedSunProfile(
            int valuePerSun,
            int sunsPerCycle,
            int plantFoodTotal
    ) {
        this.valuePerSun = valuePerSun;
        this.sunsPerCycle = sunsPerCycle;
        this.plantFoodTotal = plantFoodTotal;
    }

    @Override
    public List<Integer> getCycleDrops(long currentTick) {
        return Collections.nCopies(sunsPerCycle, valuePerSun);
    }

    @Override
    public List<Integer> getPlantFoodDrops(long currentTick) {
        int numberOfSuns = plantFoodTotal / valuePerSun;
        return Collections.nCopies(numberOfSuns, valuePerSun);
    }
}