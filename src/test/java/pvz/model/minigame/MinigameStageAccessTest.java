package pvz.model.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import pvz.model.account.User;

class MinigameStageAccessTest {
    @Test
    void stagesUnlockSequentiallyAndCompletedStagesRemainCompleted() {
        User user = user();
        MinigameProgressService service = service();
        String minigameId = MinigameCatalog.VASE_BREAKER;

        assertEquals(
                MinigameStageState.AVAILABLE,
                service.stageState(user, minigameId, 1)
        );
        assertEquals(
                MinigameStageState.LOCKED,
                service.stageState(user, minigameId, 2)
        );
        assertEquals(
                MinigameStageState.LOCKED,
                service.stageState(user, minigameId, 3)
        );

        service.recordSuccessfulCompletion(user, minigameId, 1);

        assertEquals(
                MinigameStageState.COMPLETED,
                service.stageState(user, minigameId, 1)
        );
        assertEquals(
                MinigameStageState.AVAILABLE,
                service.stageState(user, minigameId, 2)
        );
        assertEquals(
                MinigameStageState.LOCKED,
                service.stageState(user, minigameId, 3)
        );

        service.recordSuccessfulCompletion(user, minigameId, 2);

        assertEquals(
                MinigameStageState.AVAILABLE,
                service.stageState(user, minigameId, 3)
        );
        assertEquals(2, service.completedStageCount(user, minigameId));
    }

    @Test
    void cannotRecordAStageThatIsStillLocked() {
        User user = user();
        MinigameProgressService service = service();

        assertThrows(
                IllegalStateException.class,
                () -> service.recordSuccessfulCompletion(
                        user,
                        MinigameCatalog.I_ZOMBIE,
                        2
                )
        );
        assertEquals(0, service.completedStageCount(user));
    }

    private MinigameProgressService service() {
        return new MinigameProgressService(MinigameCatalog.createDefault());
    }

    private User user() {
        return new User(
                "player",
                "hash",
                "Player",
                "player@example.com",
                "x"
        );
    }
}
