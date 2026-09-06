package pvz.model.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.quest.QuestCatalog;
import pvz.model.quest.QuestEvent;
import pvz.model.quest.QuestProgress;
import pvz.model.quest.QuestSpec;
import pvz.model.quest.QuestState;
import pvz.model.service.QuestService;

/** Final Phase-2 validation for the minigame shell and Phase-7 boundary. */
class MinigameFinalIntegrationTest {
    @TempDir
    Path tempDirectory;

    @Test
    void everyConfiguredMinigameStartsWithOnlyItsFirstStageAvailable() {
        User user = user();
        MinigameCatalog catalog = MinigameCatalog.createDefault();
        MinigameProgressService progress = new MinigameProgressService(catalog);

        for (MinigameSpec spec : catalog.all()) {
            assertEquals(
                    MinigameStageState.AVAILABLE,
                    progress.stageState(
                            user,
                            MinigameStageRoute.of(spec, 1)
                    )
            );
            assertEquals(
                    MinigameStageState.LOCKED,
                    progress.stageState(
                            user,
                            MinigameStageRoute.of(spec, 2)
                    )
            );
            assertEquals(
                    MinigameStageState.LOCKED,
                    progress.stageState(
                            user,
                            MinigameStageRoute.of(spec, 3)
                    )
            );
        }
    }

    @Test
    void savedProgressRestoresSequentialStageAccessAfterReload() {
        Path saveFile = tempDirectory.resolve("users.json");
        UserManager manager = new UserManager(saveFile.toString());
        User user = user();
        manager.add(user);

        MinigameCatalog catalog = MinigameCatalog.createDefault();
        MinigameProgressService progress = new MinigameProgressService(catalog);
        MinigameSpec vaseBreaker = catalog.require(
                MinigameCatalog.VASE_BREAKER
        );

        MinigameStageRoute stageOne = MinigameStageRoute.of(vaseBreaker, 1);
        MinigameStageRoute stageTwo = MinigameStageRoute.of(vaseBreaker, 2);
        MinigameStageRoute stageThree = MinigameStageRoute.of(vaseBreaker, 3);

        MinigameProgressService.CompletionResult result =
                progress.recordSuccessfulCompletion(user, stageOne);
        assertTrue(result.changed());
        assertTrue(result.firstStageClear());
        assertEquals(1, result.minigameCompletedStageCount());
        assertEquals(1, result.totalCompletedStageCount());
        assertTrue(manager.save());

        manager.reload();
        User reloaded = manager.find(candidate ->
                candidate.getUsername().equals(user.getUsername())
        );
        assertNotNull(reloaded);

        assertEquals(
                MinigameStageState.COMPLETED,
                progress.stageState(reloaded, stageOne)
        );
        assertEquals(
                MinigameStageState.AVAILABLE,
                progress.stageState(reloaded, stageTwo)
        );
        assertEquals(
                MinigameStageState.LOCKED,
                progress.stageState(reloaded, stageThree)
        );
        assertEquals(1, progress.completedStageCount(reloaded));
    }

    @Test
    void progressAloneNeverCompletesFutureMinigameQuest() {
        User user = user();
        MinigameCatalog minigames = MinigameCatalog.createDefault();
        MinigameProgressService progress = new MinigameProgressService(
                minigames
        );
        QuestCatalog quests = QuestCatalog.createDefault();
        QuestSpec vaseQuest = quests.require(
                QuestCatalog.MINIGAME_VASE_BREAKER
        );

        progress.recordSuccessfulCompletion(
                user,
                MinigameStageRoute.of(
                        minigames.require(MinigameCatalog.VASE_BREAKER),
                        1
                )
        );

        assertEquals(1, progress.completedStageCount(user));
        assertNull(user.getQuestLog().find(vaseQuest.id()));
    }

    @Test
    void phaseSevenQuestBoundaryRequiresActivationAndMatchingMinigameId() {
        Path saveFile = tempDirectory.resolve("quest-users.json");
        UserManager manager = new UserManager(saveFile.toString());
        User user = user();
        manager.add(user);

        QuestCatalog catalog = QuestCatalog.createDefault();
        QuestSpec vaseQuest = catalog.require(
                QuestCatalog.MINIGAME_VASE_BREAKER
        );
        QuestService service = new QuestService(manager);

        assertFalse(vaseQuest.initiallyAvailable());

        service.recordEvent(
                user,
                vaseQuest,
                QuestEvent.minigameCompleted(MinigameCatalog.VASE_BREAKER)
        );
        QuestProgress progress = user.getQuestLog().find(vaseQuest.id());
        assertNotNull(progress);
        assertEquals(QuestState.UNAVAILABLE, progress.getState());
        assertEquals(0, progress.getValue());

        assertTrue(service.setAvailable(user, vaseQuest, true));
        assertEquals(QuestState.AVAILABLE, progress.getState());

        assertFalse(service.recordEvent(
                user,
                vaseQuest,
                QuestEvent.minigameCompleted(MinigameCatalog.I_ZOMBIE)
        ));
        assertEquals(0, progress.getValue());

        assertTrue(service.recordEvent(
                user,
                vaseQuest,
                QuestEvent.minigameCompleted(MinigameCatalog.VASE_BREAKER)
        ));
        assertEquals(QuestState.COMPLETED, progress.getState());
        assertEquals(1, progress.getValue());
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
