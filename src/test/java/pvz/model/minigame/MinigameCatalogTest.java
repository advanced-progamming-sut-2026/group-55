package pvz.model.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinigameCatalogTest {
    @Test
    void defaultCatalogContainsThreeRequiredMinigamesWithThreeStages() {
        MinigameCatalog catalog = MinigameCatalog.createDefault();

        assertEquals(3, catalog.size());
        assertNotNull(catalog.find(MinigameCatalog.VASE_BREAKER));
        assertNotNull(catalog.find(MinigameCatalog.WALL_NUT_BOWLING));
        assertNotNull(catalog.find(MinigameCatalog.I_ZOMBIE));
        for (MinigameSpec spec : catalog.all()) {
            assertEquals(3, spec.stageCount());
            assertTrue(spec.hasStage(1));
            assertTrue(spec.hasStage(3));
        }
    }

    @Test
    void idsAreNormalizedAndStageIdsAreStable() {
        MinigameCatalog catalog = MinigameCatalog.createDefault();
        MinigameSpec bowling = catalog.require("  WALL_NUT_BOWLING  ");

        assertEquals(MinigameCatalog.WALL_NUT_BOWLING, bowling.id());
        assertEquals("wall-nut-bowling-2", bowling.stageId(2));
        assertThrows(IllegalArgumentException.class, () -> bowling.stageId(4));
    }
}
