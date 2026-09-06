package pvz.model.leaderboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LeaderboardSortingTest {
    @Test
    void newColumnsUseLeaderboardFriendlyDefaultDirections() {
        assertEquals(
                SortDirection.ASCENDING,
                LeaderboardSort.forColumn(LeaderboardSortKey.USERNAME)
                        .direction()
        );
        assertEquals(
                SortDirection.DESCENDING,
                LeaderboardSort.forColumn(LeaderboardSortKey.MAX_MEW_POINT)
                        .direction()
        );
        assertEquals(
                LeaderboardSortKey.ADVENTURE_PROGRESS,
                LeaderboardSort.defaultSort().key()
        );
        assertEquals(
                SortDirection.DESCENDING,
                LeaderboardSort.defaultSort().direction()
        );
    }

    @Test
    void selectingSameColumnTogglesAndNewColumnResetsDirection() {
        LeaderboardSort sort = LeaderboardSort.forColumn(
                LeaderboardSortKey.MAX_MEW_POINT
        );

        sort = sort.select(LeaderboardSortKey.MAX_MEW_POINT);
        assertEquals(SortDirection.ASCENDING, sort.direction());

        sort = sort.select(LeaderboardSortKey.USERNAME);
        assertEquals(LeaderboardSortKey.USERNAME, sort.key());
        assertEquals(SortDirection.ASCENDING, sort.direction());
    }

    @Test
    void everyNumericColumnSortsBothDirections() {
        LeaderboardService service = service();

        assertUsernames(
                service.snapshot(
                        LeaderboardSortKey.MINIGAME_COMPLETIONS,
                        SortDirection.ASCENDING
                ),
                "alpha", "bravo", "charlie", "delta"
        );
        assertUsernames(
                service.snapshot(
                        LeaderboardSortKey.MINIGAME_COMPLETIONS,
                        SortDirection.DESCENDING
                ),
                "delta", "charlie", "bravo", "alpha"
        );

        assertUsernames(
                service.snapshot(
                        LeaderboardSortKey.DAILY_QUEST_COMPLETIONS,
                        SortDirection.ASCENDING
                ),
                "delta", "charlie", "bravo", "alpha"
        );
        assertUsernames(
                service.snapshot(
                        LeaderboardSortKey.DAILY_QUEST_COMPLETIONS,
                        SortDirection.DESCENDING
                ),
                "alpha", "bravo", "charlie", "delta"
        );

        assertUsernames(
                service.snapshot(
                        LeaderboardSortKey.NON_DAILY_QUEST_COMPLETIONS,
                        SortDirection.ASCENDING
                ),
                "alpha", "delta", "bravo", "charlie"
        );
        assertUsernames(
                service.snapshot(
                        LeaderboardSortKey.NON_DAILY_QUEST_COMPLETIONS,
                        SortDirection.DESCENDING
                ),
                "charlie", "bravo", "alpha", "delta"
        );

        assertUsernames(
                service.snapshot(
                        LeaderboardSortKey.MAX_MEW_POINT,
                        SortDirection.ASCENDING
                ),
                "bravo", "charlie", "delta", "alpha"
        );
        assertUsernames(
                service.snapshot(
                        LeaderboardSortKey.MAX_MEW_POINT,
                        SortDirection.DESCENDING
                ),
                "alpha", "delta", "charlie", "bravo"
        );
    }

    @Test
    void adventureSortUsesChapterThenLevelAndKeepsNoProgressAtBottomDescending() {
        LeaderboardService service = service();

        assertUsernames(
                service.snapshot(
                        LeaderboardSortKey.ADVENTURE_PROGRESS,
                        SortDirection.DESCENDING
                ),
                "delta", "charlie", "bravo", "alpha"
        );
        assertUsernames(
                service.snapshot(
                        LeaderboardSortKey.ADVENTURE_PROGRESS,
                        SortDirection.ASCENDING
                ),
                "alpha", "bravo", "charlie", "delta"
        );
    }

    @Test
    void usernameSortIsCaseInsensitiveWithDeterministicCaseFallback() {
        LeaderboardService service = new LeaderboardService(() -> List.of(
                entry("bravo", none(), 0, 0, 0, 0),
                entry("Alpha", none(), 0, 0, 0, 0),
                entry("alpha", none(), 0, 0, 0, 0)
        ));

        assertUsernames(
                service.snapshot(
                        LeaderboardSortKey.USERNAME,
                        SortDirection.ASCENDING
                ),
                "Alpha", "alpha", "bravo"
        );
        assertUsernames(
                service.snapshot(
                        LeaderboardSortKey.USERNAME,
                        SortDirection.DESCENDING
                ),
                "bravo", "alpha", "Alpha"
        );
    }

    @Test
    void equalStatisticsAlwaysTieBreakByUsernameAscending() {
        LeaderboardService service = new LeaderboardService(() -> List.of(
                entry("zulu", none(), 5, 2, 3, 100),
                entry("alpha", none(), 5, 2, 3, 100),
                entry("mike", none(), 5, 2, 3, 100)
        ));

        for (LeaderboardSortKey key : List.of(
                LeaderboardSortKey.ADVENTURE_PROGRESS,
                LeaderboardSortKey.MINIGAME_COMPLETIONS,
                LeaderboardSortKey.DAILY_QUEST_COMPLETIONS,
                LeaderboardSortKey.NON_DAILY_QUEST_COMPLETIONS,
                LeaderboardSortKey.MAX_MEW_POINT
        )) {
            assertUsernames(
                    service.snapshot(key, SortDirection.DESCENDING),
                    "alpha", "mike", "zulu"
            );
            assertUsernames(
                    service.snapshot(key, SortDirection.ASCENDING),
                    "alpha", "mike", "zulu"
            );
        }
    }

    @Test
    void sortingNeverMutatesDataSourceOrder() {
        List<LeaderboardEntry> original = List.of(
                entry("charlie", none(), 0, 0, 0, 10),
                entry("alpha", none(), 0, 0, 0, 30),
                entry("bravo", none(), 0, 0, 0, 20)
        );
        LeaderboardService service = new LeaderboardService(() -> original);

        service.snapshot(
                LeaderboardSortKey.MAX_MEW_POINT,
                SortDirection.DESCENDING
        );

        assertUsernames(service.snapshot(), "charlie", "alpha", "bravo");
    }

    private LeaderboardService service() {
        return new LeaderboardService(() -> List.of(
                entry("delta", standing(3, 1), 4, 1, 1, 300),
                entry("alpha", none(), 1, 4, 1, 400),
                entry("charlie", standing(2, 3), 3, 2, 4, 200),
                entry("bravo", standing(2, 1), 2, 3, 2, 100)
        ));
    }

    private LeaderboardEntry entry(
            String username,
            AdventureStanding adventure,
            int minigames,
            int daily,
            int nonDaily,
            int mewPoint
    ) {
        return new LeaderboardEntry(
                username,
                username,
                adventure,
                minigames,
                daily,
                nonDaily,
                mewPoint
        );
    }

    private AdventureStanding standing(int chapter, int level) {
        return new AdventureStanding(
                "chapter-" + chapter,
                "Chapter " + chapter,
                chapter,
                "level-" + chapter + "-" + level,
                "Level " + level,
                level
        );
    }

    private AdventureStanding none() {
        return AdventureStanding.none();
    }

    private void assertUsernames(
            List<LeaderboardEntry> entries,
            String... expected
    ) {
        assertEquals(
                List.of(expected),
                entries.stream().map(LeaderboardEntry::username).toList()
        );
    }
}
