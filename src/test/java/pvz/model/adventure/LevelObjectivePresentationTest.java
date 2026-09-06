package pvz.model.adventure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LevelObjectivePresentationTest {
    @Test
    void normalClearAllWavesLevelHasPlayerFacingObjectiveCopy() {
        LevelSpec level = new LevelSpec(
                "egypt-1",
                "ancient-egypt",
                1,
                "Ancient Egypt - Day 1",
                LevelType.NORMAL,
                9,
                5,
                100,
                true,
                ObjectiveType.CLEAR_ALL_WAVES
        );

        LevelObjectivePresentation presentation =
                new LevelEntryRouter().objectivePresentation(level);

        assertEquals("DEFEND THE HOUSE", presentation.title());
        assertTrue(presentation.description().contains("zombie wave"));
        assertTrue(presentation.description().contains("reach the house"));
    }
}
