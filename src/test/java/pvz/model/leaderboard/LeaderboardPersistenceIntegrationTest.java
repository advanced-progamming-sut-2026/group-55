package pvz.model.leaderboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.adventure.ChapterSpec;
import pvz.model.adventure.LevelCatalog;
import pvz.model.adventure.LevelSpec;
import pvz.model.adventure.LevelType;
import pvz.model.adventure.ObjectiveType;
import pvz.model.minigame.MinigameCatalog;
import pvz.model.minigame.MinigameProgressService;
import pvz.model.quest.QuestCatalog;

class LeaderboardPersistenceIntegrationTest {
    @TempDir
    Path tempDirectory;

    @Test
    void persistedUsersProduceStableLeaderboardRowsAfterReload() {
        Path save = tempDirectory.resolve("users.json");
        UserManager manager = new UserManager(save.toString());

        User alpha = user("alpha", "Alpha");
        User bravo = user("bravo", "Bravo");
        bravo.getAdventureProgress().completeLevel("egypt-1");
        bravo.getQuestLog().getOrCreate(QuestCatalog.DAILY_PLAY_ONE)
                .markCompleted();
        new MinigameProgressService(MinigameCatalog.createDefault())
                .recordSuccessfulCompletion(
                        bravo,
                        MinigameCatalog.VASE_BREAKER,
                        1
                );
        bravo.setMaxMewPoint(50);

        User charlie = user("charlie", "Charlie");
        charlie.getAdventureProgress().completeLevel("egypt-2");
        charlie.setMaxMewPoint(100);

        manager.add(alpha);
        manager.add(bravo);
        manager.add(charlie);
        org.junit.jupiter.api.Assertions.assertTrue(manager.save());
        manager.reload();

        LeaderboardService service = new LeaderboardService(
                new LocalLeaderboardDataSource(
                        manager,
                        catalog(),
                        QuestCatalog.createDefault()
                )
        );

        List<LeaderboardEntry> defaultRows = service.snapshot(
                LeaderboardSort.defaultSort()
        );
        assertEquals(
                List.of("charlie", "bravo", "alpha"),
                usernames(defaultRows)
        );
        assertFalse(defaultRows.get(2).adventure().hasProgress());

        LeaderboardEntry bravoRow = defaultRows.stream()
                .filter(row -> row.username().equals("bravo"))
                .findFirst()
                .orElseThrow();
        assertEquals(1, bravoRow.completedMinigameStages());
        assertEquals(1, bravoRow.completedDailyQuests());
        assertEquals(50, bravoRow.maxMewPoint());

        assertEquals(
                List.of("alpha", "bravo", "charlie"),
                usernames(service.snapshot(
                        LeaderboardSortKey.USERNAME,
                        SortDirection.ASCENDING
                ))
        );
    }

    @Test
    void oldSaveWithoutNewProgressFieldsStillProducesLeaderboardRow()
            throws Exception {
        Path save = tempDirectory.resolve("old-users.json");
        Files.writeString(
                save,
                """
                [
                  {
                    "username": "legacy-player",
                    "passwordHash": "hash",
                    "nickname": "Legacy Player",
                    "email": "legacy@example.com",
                    "gender": "x"
                  }
                ]
                """
        );

        UserManager manager = new UserManager(save.toString());
        LeaderboardService service = new LeaderboardService(
                new LocalLeaderboardDataSource(
                        manager,
                        catalog(),
                        QuestCatalog.createDefault()
                )
        );

        List<LeaderboardEntry> rows = service.snapshot(
                LeaderboardSort.defaultSort()
        );
        assertEquals(1, rows.size());
        LeaderboardEntry row = rows.get(0);
        assertEquals("legacy-player", row.username());
        assertFalse(row.adventure().hasProgress());
        assertEquals(0, row.completedMinigameStages());
        assertEquals(0, row.completedDailyQuests());
        assertEquals(0, row.completedNonDailyQuests());
        assertEquals(0, row.maxMewPoint());
    }

    private List<String> usernames(List<LeaderboardEntry> rows) {
        return rows.stream().map(LeaderboardEntry::username).toList();
    }

    private User user(String username, String nickname) {
        return new User(
                username,
                "hash",
                nickname,
                username + "@example.com",
                "x"
        );
    }

    private LevelCatalog catalog() {
        Map<String, ChapterSpec> chapters = new LinkedHashMap<>();
        chapters.put(
                "egypt",
                new ChapterSpec("egypt", "Ancient Egypt", 1)
        );

        Map<String, LevelSpec> levels = new LinkedHashMap<>();
        levels.put("egypt-1", level("egypt-1", 1));
        levels.put("egypt-2", level("egypt-2", 2));
        return new LevelCatalog(chapters, levels);
    }

    private LevelSpec level(String id, int number) {
        return new LevelSpec(
                id,
                "egypt",
                number,
                "Stage " + number,
                LevelType.NORMAL,
                9,
                5,
                50,
                true,
                ObjectiveType.CLEAR_ALL_WAVES
        );
    }
}
