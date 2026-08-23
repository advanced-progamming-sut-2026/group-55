package pvz.model.adventure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pvz.model.account.User;

class LevelProgressServiceTest {
    private LevelCatalog catalog;
    private LevelProgressService service;
    private User user;

    @BeforeEach
    void setUp() {
        Map<String, ChapterSpec> chapters = new LinkedHashMap<>();
        chapters.put("ancient-egypt", new ChapterSpec(
                "ancient-egypt",
                "Ancient Egypt",
                1
        ));
        chapters.put("frostbite-caves", new ChapterSpec(
                "frostbite-caves",
                "Frostbite Caves",
                2
        ));

        Map<String, LevelSpec> levels = new LinkedHashMap<>();
        levels.put("egypt-1", normalLevel("egypt-1", "ancient-egypt", 1));
        levels.put("egypt-2", normalLevel("egypt-2", "ancient-egypt", 2));
        levels.put(
                "frostbite-1",
                normalLevel("frostbite-1", "frostbite-caves", 1)
        );

        catalog = new LevelCatalog(chapters, levels);
        service = new LevelProgressService(catalog);
        user = new User("player", "hash", "Player", "p@example.com", "x");
    }

    @Test
    void unlocksLevelsSequentiallyAndDoesNotDoubleCountReplay() {
        LevelSpec first = catalog.requireLevel("egypt-1");
        LevelSpec second = catalog.requireLevel("egypt-2");

        assertEquals(LevelProgressService.LevelState.AVAILABLE,
                service.state(user, first));
        assertEquals(LevelProgressService.LevelState.LOCKED,
                service.state(user, second));
        assertThrows(
                IllegalStateException.class,
                () -> service.completeLevel(user, second.id())
        );

        LevelProgressService.CompletionResult firstResult =
                service.completeLevel(user, first.id());

        assertTrue(firstResult.newlyCompleted());
        assertEquals("egypt-2", firstResult.unlockedLevelId());
        assertEquals(1, user.getClearedStages());
        assertTrue(service.isUnlocked(user, second));

        LevelProgressService.CompletionResult replayResult =
                service.completeLevel(user, first.id());

        assertFalse(replayResult.newlyCompleted());
        assertEquals(1, user.getClearedStages());
    }

    @Test
    void unlocksNextChapterOnlyAfterCurrentChapterIsCompleted() {
        service.completeLevel(user, "egypt-1");
        LevelProgressService.CompletionResult result =
                service.completeLevel(user, "egypt-2");

        assertEquals("frostbite-caves", result.unlockedChapterId());
        assertEquals("frostbite-1", result.unlockedLevelId());
        assertTrue(user.isChapterUnlocked("frostbite-caves"));
        assertTrue(service.isUnlocked(
                user,
                catalog.requireLevel("frostbite-1")
        ));
    }

    @Test
    void doesNotUnlockAnEmptyFollowingChapter() {
        Map<String, ChapterSpec> chapters = new LinkedHashMap<>();
        chapters.put("ancient-egypt", new ChapterSpec(
                "ancient-egypt",
                "Ancient Egypt",
                1
        ));
        chapters.put("empty-world", new ChapterSpec(
                "empty-world",
                "Empty World",
                2
        ));
        LevelSpec onlyLevel = normalLevel(
                "egypt-1",
                "ancient-egypt",
                1
        );
        LevelProgressService localService = new LevelProgressService(
                new LevelCatalog(chapters, Map.of("egypt-1", onlyLevel))
        );

        LevelProgressService.CompletionResult result =
                localService.completeLevel(user, "egypt-1");

        assertNull(result.unlockedChapterId());
        assertFalse(user.isChapterUnlocked("empty-world"));
    }

    private LevelSpec normalLevel(
            String id,
            String chapterId,
            int number
    ) {
        return new LevelSpec(
                id,
                chapterId,
                number,
                id,
                LevelType.NORMAL,
                9,
                5,
                50,
                true,
                ObjectiveType.CLEAR_ALL_WAVES
        );
    }
}
