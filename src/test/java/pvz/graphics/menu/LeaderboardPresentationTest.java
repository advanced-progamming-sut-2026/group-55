package pvz.graphics.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import pvz.model.leaderboard.AdventureStanding;
import pvz.model.leaderboard.LeaderboardEntry;

class LeaderboardPresentationTest {
    @Test
    void shortPlayerTextIsPreserved() {
        LeaderboardEntry entry = entry("player-one", "Player One");

        assertEquals("player-one", LeaderboardPresentation.username(entry));
        assertEquals(
                "Player One",
                LeaderboardPresentation.secondaryPlayerText(entry, false)
        );
        assertEquals(
                "Player One - YOU",
                LeaderboardPresentation.secondaryPlayerText(entry, true)
        );
    }

    @Test
    void longPlayerTextIsBoundedWithoutLosingCurrentUserMarker() {
        LeaderboardEntry entry = entry(
                "this-is-a-very-very-long-username-for-a-player",
                "This nickname is also much longer than the leaderboard cell"
        );

        String username = LeaderboardPresentation.username(entry);
        String secondary = LeaderboardPresentation.secondaryPlayerText(
                entry,
                true
        );

        assertTrue(
                username.length()
                        <= LeaderboardPresentation.USERNAME_MAX_CHARS
        );
        assertTrue(username.endsWith("..."));
        assertTrue(
                secondary.length()
                        <= LeaderboardPresentation.NICKNAME_MAX_CHARS
        );
        assertTrue(secondary.endsWith(" - YOU"));
        assertTrue(secondary.contains("..."));
    }

    @Test
    void identicalNicknameUsesOnlyCurrentUserMarker() {
        LeaderboardEntry entry = entry("same-name", "same-name");

        assertEquals(
                "",
                LeaderboardPresentation.secondaryPlayerText(entry, false)
        );
        assertEquals(
                "YOU",
                LeaderboardPresentation.secondaryPlayerText(entry, true)
        );
    }

    @Test
    void adventureTextHandlesNoProgressAndBoundsLongNames() {
        assertEquals(
                "-",
                LeaderboardPresentation.adventure(AdventureStanding.none())
        );

        AdventureStanding standing = new AdventureStanding(
                "chapter-id",
                "A chapter name that is intentionally far too long to fit",
                1,
                "level-id",
                "A level name that is intentionally far too long to fit cleanly",
                1
        );

        String[] lines = LeaderboardPresentation.adventure(standing)
                .split("\\n", -1);
        assertEquals(2, lines.length);
        assertTrue(
                lines[0].length()
                        <= LeaderboardPresentation.ADVENTURE_CHAPTER_MAX_CHARS
        );
        assertTrue(
                lines[1].length()
                        <= LeaderboardPresentation.ADVENTURE_LEVEL_MAX_CHARS
        );
        assertTrue(lines[0].endsWith("..."));
        assertTrue(lines[1].endsWith("..."));
    }

    @Test
    void ellipsizeRejectsUnusableLimits() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LeaderboardPresentation.ellipsize("value", 3)
        );
    }

    private LeaderboardEntry entry(String username, String nickname) {
        return new LeaderboardEntry(
                username,
                nickname,
                AdventureStanding.none(),
                0,
                0,
                0,
                0
        );
    }
}
