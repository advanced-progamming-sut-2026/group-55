package pvz.model.leaderboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LeaderboardModelTest {
    @Test
    void noAdventureStandingHasNoProgress() {
        AdventureStanding standing = AdventureStanding.none();

        assertFalse(standing.hasProgress());
        assertEquals(0, standing.chapterOrder());
        assertEquals(0, standing.levelNumber());
    }

    @Test
    void entryNormalizesNicknameAndRejectsNegativeStatistics() {
        LeaderboardEntry entry = new LeaderboardEntry(
                " player ",
                "   ",
                AdventureStanding.none(),
                0,
                0,
                0,
                0
        );

        assertEquals("player", entry.username());
        assertEquals("player", entry.nickname());
        assertThrows(
                IllegalArgumentException.class,
                () -> new LeaderboardEntry(
                        "player",
                        "Player",
                        AdventureStanding.none(),
                        -1,
                        0,
                        0,
                        0
                )
        );
    }

    @Test
    void sortDirectionTogglesBothWays() {
        assertEquals(
                SortDirection.DESCENDING,
                SortDirection.ASCENDING.toggled()
        );
        assertEquals(
                SortDirection.ASCENDING,
                SortDirection.DESCENDING.toggled()
        );
    }

    @Test
    void populatedAdventureStandingRequiresCompleteCoordinates() {
        AdventureStanding standing = new AdventureStanding(
                "ancient-egypt",
                "Ancient Egypt",
                1,
                "egypt-2",
                "Stage 2",
                2
        );

        assertTrue(standing.hasProgress());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AdventureStanding(
                        "ancient-egypt",
                        "Ancient Egypt",
                        1,
                        null,
                        null,
                        0
                )
        );
    }
}
