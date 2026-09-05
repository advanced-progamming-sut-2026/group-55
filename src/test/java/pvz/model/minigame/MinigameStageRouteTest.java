package pvz.model.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import pvz.model.account.User;

class MinigameStageRouteTest {
    @Test
    void routeUsesCanonicalIdAndValidatedStage() {
        MinigameCatalog catalog = MinigameCatalog.createDefault();
        MinigameSpec spec = catalog.require("VASE_BREAKER");

        MinigameStageRoute route = MinigameStageRoute.of(spec, 2);

        assertEquals(MinigameCatalog.VASE_BREAKER, route.minigameId());
        assertEquals(2, route.stageNumber());
        assertEquals("vase-breaker-2", route.stageId(catalog));
        assertThrows(
                IllegalArgumentException.class,
                () -> MinigameStageRoute.of(spec, 4)
        );
    }

    @Test
    void progressServiceAcceptsRouteWithoutChangingSequentialRules() {
        MinigameCatalog catalog = MinigameCatalog.createDefault();
        MinigameProgressService service = new MinigameProgressService(catalog);
        User user = new User(
                "player",
                "hash",
                "Player",
                "player@example.com",
                "x"
        );
        MinigameSpec spec = catalog.require(MinigameCatalog.I_ZOMBIE);
        MinigameStageRoute stage1 = MinigameStageRoute.of(spec, 1);
        MinigameStageRoute stage2 = MinigameStageRoute.of(spec, 2);

        assertEquals(
                MinigameStageState.AVAILABLE,
                service.stageState(user, stage1)
        );
        assertEquals(
                MinigameStageState.LOCKED,
                service.stageState(user, stage2)
        );

        service.recordSuccessfulCompletion(user, stage1);

        assertEquals(
                MinigameStageState.AVAILABLE,
                service.stageState(user, stage2)
        );
        assertEquals(true, service.isStageCompleted(user, stage1));
    }
}
