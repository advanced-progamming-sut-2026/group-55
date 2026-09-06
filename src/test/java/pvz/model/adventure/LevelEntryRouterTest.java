package pvz.model.adventure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LevelEntryRouterTest {
    private final LevelEntryRouter router = new LevelEntryRouter();

    @Test
    void normalLevelUsesPlantSelectionRoute() {
        assertEquals(
                LevelEntryRoute.PLANT_SELECTION,
                router.route(level(LevelType.NORMAL))
        );
    }

    @Test
    void futureSetupStrategyCanBeRegisteredWithoutChangingTheScreen() {
        LevelEntryRouter configurable = new LevelEntryRouter(Map.of(
                LevelType.SPECIAL,
                level -> LevelEntryRoute.PLANT_SELECTION
        ));

        assertEquals(
                LevelEntryRoute.PLANT_SELECTION,
                configurable.route(level(LevelType.SPECIAL))
        );
    }

    @Test
    void specialLevelCannotSilentlyUseNormalPlantSelection() {
        assertEquals(
                LevelEntryRoute.UNSUPPORTED_SETUP,
                router.route(level(LevelType.SPECIAL))
        );
    }

    private LevelSpec level(LevelType type) {
        return new LevelSpec(
                "test-level",
                "ancient-egypt",
                1,
                "Test Level",
                type,
                9,
                5,
                50,
                true,
                ObjectiveType.CLEAR_ALL_WAVES
        );
    }
}
