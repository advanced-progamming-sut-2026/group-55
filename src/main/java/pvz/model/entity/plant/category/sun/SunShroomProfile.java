package pvz.model.entity.plant.category.sun;

import java.util.List;
import pvz.model.core.Game;
import pvz.model.entity.collectible.sun.SunValue;

public final class SunShroomProfile implements SunProfile {
    private static final int BASE_SECOND_STAGE_SECONDS = 24;
    private static final int BASE_FINAL_STAGE_SECONDS = 72;

    private final long plantedTick;
    private final int secondStageSeconds;
    private final int finalStageSeconds;
    private boolean forcedFinalStage;

    public SunShroomProfile(long plantedTick, int growTimeAdjustmentSeconds) {
        this.plantedTick = plantedTick;
        this.secondStageSeconds = Math.max(1, BASE_SECOND_STAGE_SECONDS + growTimeAdjustmentSeconds);
        this.finalStageSeconds = Math.max(secondStageSeconds + 1,
                BASE_FINAL_STAGE_SECONDS + growTimeAdjustmentSeconds);
    }

    public SunShroomProfile(long plantedTick) {
        this(plantedTick, 0);
    }

    public int getCurrentStage(long currentTick) {
        if (forcedFinalStage) {
            return 3;
        }
        long ageInSeconds = (currentTick - plantedTick) / Game.TICKS_PER_SECOND;
        if (ageInSeconds <= secondStageSeconds) {
            return 1;
        }
        if (ageInSeconds < finalStageSeconds) {
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
