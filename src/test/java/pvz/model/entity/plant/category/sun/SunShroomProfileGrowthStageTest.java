package pvz.model.entity.plant.category.sun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import pvz.model.core.Game;

class SunShroomProfileGrowthStageTest {

    @Test
    void reportsTheSameThreeStagesUsedBySunProductionLogic() {
        SunShroomProfile profile = new SunShroomProfile(0L);

        assertEquals(1, profile.getGrowthStage(0L));
        assertEquals(
                1,
                profile.getGrowthStage(24L * Game.TICKS_PER_SECOND)
        );
        assertEquals(
                2,
                profile.getGrowthStage(25L * Game.TICKS_PER_SECOND)
        );
        assertEquals(
                3,
                profile.getGrowthStage(72L * Game.TICKS_PER_SECOND)
        );
    }

    @Test
    void plantFoodForcesThePresentationStageToFinalStageImmediately() {
        SunShroomProfile profile = new SunShroomProfile(0L);

        profile.applyPlantFoodEffect();

        assertEquals(3, profile.getGrowthStage(0L));
    }
}
