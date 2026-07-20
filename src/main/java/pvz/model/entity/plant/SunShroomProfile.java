package pvz.model.entity.plant;

import java.util.List;

import pvz.model.core.Game;
import pvz.model.entity.collectible.sun.SunValue;

public final class SunShroomProfile implements SunProfile {
    private static final int SECOND_STAGE_SECONDS = 48;
    private static final int FINAL_STAGE_SECONDS = 96;

    private final long plantedTick;
    private boolean forcedFinalStage;

    public SunShroomProfile(long plantedTick) {
        this.plantedTick = plantedTick;
    }

    public int getCurrentStage(long currentTick) {
        if (forcedFinalStage) {
            return 3;
        }

        long ageInSeconds =
                (currentTick - plantedTick) / Game.TICKS_PER_SECOND;

        if (ageInSeconds < SECOND_STAGE_SECONDS) {
            return 1;
        }

        if (ageInSeconds < FINAL_STAGE_SECONDS) {
            return 2;
        }

        return 3;
    }

    @Override
    public List<Integer> getCycleDrops(long currentTick) {
        int value = switch (getCurrentStage(currentTick)) {
            case 1 -> SunValue.SMALLSUN.getValue();
            case 2 -> SunValue.NORMALSUN.getValue();
            default -> SunValue.BIGSUN.getValue();
        };

        return List.of(value);
    }

    @Override
    public List<Integer> getPlantFoodDrops(long currentTick) {
        applyPlantFoodEffect();
        int sunValue = SunValue.BIGSUN.getValue();
        return List.of(sunValue, sunValue, sunValue);
    }

    @Override
    public void applyPlantFoodEffect() {
        forcedFinalStage = true;
    }
}