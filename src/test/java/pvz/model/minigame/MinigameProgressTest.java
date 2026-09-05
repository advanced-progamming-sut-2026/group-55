package pvz.model.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import pvz.model.account.User;

class MinigameProgressTest {
    @Test
    void recordsUniqueStageProgressAndIgnoresReplays() {
        User user = user();
        MinigameProgressService service = new MinigameProgressService(
                MinigameCatalog.createDefault()
        );

        var first = service.recordSuccessfulCompletion(
                user,
                MinigameCatalog.VASE_BREAKER,
                1
        );
        var replay = service.recordSuccessfulCompletion(
                user,
                MinigameCatalog.VASE_BREAKER,
                1
        );
        var secondStage = service.recordSuccessfulCompletion(
                user,
                MinigameCatalog.VASE_BREAKER,
                2
        );

        assertTrue(first.firstStageClear());
        assertFalse(replay.firstStageClear());
        assertTrue(secondStage.firstStageClear());
        assertFalse(replay.changed());
        assertEquals(2, service.completedStageCount(user));
        assertEquals(2, user.getMinigameProgress().getCompletedStageCount());
        assertTrue(service.isStageCompleted(
                user,
                MinigameCatalog.VASE_BREAKER,
                1
        ));
        assertTrue(service.isStageCompleted(
                user,
                MinigameCatalog.VASE_BREAKER,
                2
        ));
        assertFalse(service.isStageCompleted(
                user,
                MinigameCatalog.VASE_BREAKER,
                3
        ));
    }

    @Test
    void rejectsUnknownMinigamesAndInvalidStages() {
        User user = user();
        MinigameProgressService service = new MinigameProgressService(
                MinigameCatalog.createDefault()
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.recordSuccessfulCompletion(user, "unknown", 1)
        );
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.recordSuccessfulCompletion(
                        user,
                        MinigameCatalog.I_ZOMBIE,
                        4
                )
        );
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
