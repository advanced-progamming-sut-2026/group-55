package pvz.model.leaderboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
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

class LocalLeaderboardDataSourceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void buildsRowsForAllUsersFromCurrentPersistentState() {
        UserManager manager = new UserManager(
                tempDirectory.resolve("users.json").toString()
        );
        User first = user("first", "First Player");
        first.getAdventureProgress().completeLevel("egypt-2");
        first.getAdventureProgress().completeLevel("frost-1");
        first.getAdventureProgress().completeLevel("unknown-old-level");
        first.setMaxMewPoint(420);
        var daily = first.getQuestLog()
                .getOrCreate(QuestCatalog.DAILY_PLAY_ONE);
        daily.markCompleted();
        daily.resetForCycle(LocalDate.of(2026, 9, 6));
        daily.markCompleted();
        first.getQuestLog()
                .getOrCreate(QuestCatalog.ADVENTURE_FIRST_CLEAR)
                .markCompleted();

        MinigameProgressService minigames = new MinigameProgressService(
                MinigameCatalog.createDefault()
        );
        minigames.recordSuccessfulCompletion(
                first,
                MinigameCatalog.VASE_BREAKER,
                1
        );
        minigames.recordSuccessfulCompletion(
                first,
                MinigameCatalog.VASE_BREAKER,
                1
        );
        minigames.recordSuccessfulCompletion(
                first,
                MinigameCatalog.VASE_BREAKER,
                2
        );

        User second = user("second", "Second Player");
        manager.add(first);
        manager.add(second);

        LocalLeaderboardDataSource source =
                new LocalLeaderboardDataSource(
                        manager,
                        catalog(),
                        QuestCatalog.createDefault()
                );

        List<LeaderboardEntry> entries = source.loadEntries();
        assertEquals(2, entries.size());

        LeaderboardEntry firstEntry = entries.stream()
                .filter(entry -> entry.username().equals("first"))
                .findFirst()
                .orElseThrow();
        assertTrue(firstEntry.adventure().hasProgress());
        assertEquals("frost", firstEntry.adventure().chapterId());
        assertEquals("frost-1", firstEntry.adventure().levelId());
        assertEquals(2, firstEntry.completedDailyQuests());
        assertEquals(1, firstEntry.completedNonDailyQuests());
        assertEquals(2, firstEntry.completedMinigameStages());
        assertEquals(420, firstEntry.maxMewPoint());

        LeaderboardEntry secondEntry = entries.stream()
                .filter(entry -> entry.username().equals("second"))
                .findFirst()
                .orElseThrow();
        assertFalse(secondEntry.adventure().hasProgress());
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
        chapters.put(
                "frost",
                new ChapterSpec("frost", "Frostbite Caves", 2)
        );

        Map<String, LevelSpec> levels = new LinkedHashMap<>();
        levels.put("egypt-1", level("egypt-1", "egypt", 1));
        levels.put("egypt-2", level("egypt-2", "egypt", 2));
        levels.put("frost-1", level("frost-1", "frost", 1));
        return new LevelCatalog(chapters, levels);
    }

    private LevelSpec level(String id, String chapterId, int number) {
        return new LevelSpec(
                id,
                chapterId,
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
