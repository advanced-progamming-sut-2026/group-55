package pvz.graphics.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SunShroomAnimationControllerTest {

    @Test
    void stageChangePlaysGrowthBeforeNewIdle() {
        SunShroomAnimationController controller =
                new SunShroomAnimationController();

        assertSelection(
                controller.select(1, false, Long.MIN_VALUE, 0f),
                "idle_stage1",
                true
        );
        assertSelection(
                controller.select(2, false, Long.MIN_VALUE, 0f),
                "growth_stage1",
                false
        );
        assertSelection(
                controller.select(2, false, Long.MIN_VALUE, 1.34f),
                "idle_stage2",
                true
        );
    }

    @Test
    void skippedRenderStageStillShowsBothGrowthTransitions() {
        SunShroomAnimationController controller =
                new SunShroomAnimationController();

        controller.select(1, false, Long.MIN_VALUE, 0f);

        assertSelection(
                controller.select(3, false, Long.MIN_VALUE, 0f),
                "growth_stage1",
                false
        );
        assertSelection(
                controller.select(3, false, Long.MIN_VALUE, 1.34f),
                "growth_stage2",
                false
        );
        assertSelection(
                controller.select(3, false, Long.MIN_VALUE, 1.34f),
                "idle_stage3",
                true
        );
    }

    @Test
    void productionUsesTheStageThatProducedTheSun() {
        SunShroomAnimationController controller =
                new SunShroomAnimationController();

        controller.select(2, false, Long.MIN_VALUE, 0f);

        assertSelection(
                controller.select(2, false, 100L, 0f),
                "special_stage2",
                false
        );
        assertSelection(
                controller.select(2, false, 100L, 1.84f),
                "idle_stage2",
                true
        );
    }

    @Test
    void productionDuringGrowthIsQueuedUntilGrowthFinishes() {
        SunShroomAnimationController controller =
                new SunShroomAnimationController();

        controller.select(1, false, Long.MIN_VALUE, 0f);

        assertSelection(
                controller.select(2, false, 240L, 0f),
                "growth_stage1",
                false
        );
        assertSelection(
                controller.select(2, false, 240L, 1.34f),
                "special_stage2",
                false
        );
    }

    @Test
    void plantFoodImmediatelyUsesForcedFinalStageAndDoesNotReplayGrowth() {
        SunShroomAnimationController controller =
                new SunShroomAnimationController();

        controller.select(1, false, Long.MIN_VALUE, 0f);

        SunShroomAnimationController.Selection plantFood =
                controller.select(3, true, Long.MIN_VALUE, 0f);
        assertSelection(plantFood, "plantfood_stage3", false);

        SunShroomAnimationController.Selection after =
                controller.select(3, false, Long.MIN_VALUE, 0f);
        assertSelection(after, "idle_stage3", true);
    }

    @Test
    void everyStageUsesFinalIdleAsStableFitReference() {
        SunShroomAnimationController controller =
                new SunShroomAnimationController();

        SunShroomAnimationController.Selection stageOne =
                controller.select(1, false, Long.MIN_VALUE, 0f);

        assertEquals("idle_stage3", stageOne.referenceClip());
        assertTrue(SunShroomAnimationController.supports("Sun-Shroom"));
        assertFalse(SunShroomAnimationController.supports("Sunflower"));
    }

    private void assertSelection(
            SunShroomAnimationController.Selection selection,
            String expectedClip,
            boolean expectedLoop
    ) {
        assertEquals(expectedClip, selection.clip());
        assertEquals("idle_stage3", selection.referenceClip());
        assertEquals(expectedLoop, selection.loop());
    }
}
