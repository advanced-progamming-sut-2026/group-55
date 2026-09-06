package pvz.graphics.menu;

import java.util.Objects;
import pvz.model.leaderboard.AdventureStanding;
import pvz.model.leaderboard.LeaderboardEntry;

/** Pure display formatting rules shared by the leaderboard rows. */
final class LeaderboardPresentation {
    static final int USERNAME_MAX_CHARS = 24;
    static final int NICKNAME_MAX_CHARS = 28;
    static final int ADVENTURE_CHAPTER_MAX_CHARS = 30;
    static final int ADVENTURE_LEVEL_MAX_CHARS = 32;

    private static final String ELLIPSIS = "...";

    private LeaderboardPresentation() {
    }

    static String username(LeaderboardEntry entry) {
        Objects.requireNonNull(entry, "leaderboard entry cannot be null");
        return ellipsize(entry.username(), USERNAME_MAX_CHARS);
    }

    static String secondaryPlayerText(
            LeaderboardEntry entry,
            boolean currentUser
    ) {
        Objects.requireNonNull(entry, "leaderboard entry cannot be null");

        if (entry.nickname().equals(entry.username())) {
            return currentUser ? "YOU" : "";
        }

        String suffix = currentUser ? " - YOU" : "";
        return ellipsizeWithSuffix(
                entry.nickname(),
                suffix,
                NICKNAME_MAX_CHARS
        );
    }

    static String adventure(AdventureStanding standing) {
        if (standing == null || !standing.hasProgress()) {
            return "-";
        }

        return ellipsize(
                standing.chapterName(),
                ADVENTURE_CHAPTER_MAX_CHARS
        ) + "\n" + ellipsize(
                standing.levelName(),
                ADVENTURE_LEVEL_MAX_CHARS
        );
    }

    static String ellipsize(String value, int maxChars) {
        Objects.requireNonNull(value, "display text cannot be null");
        if (maxChars < ELLIPSIS.length() + 1) {
            throw new IllegalArgumentException(
                    "maximum display length is too small"
            );
        }

        String checked = value.strip();
        if (checked.length() <= maxChars) {
            return checked;
        }

        return checked.substring(0, maxChars - ELLIPSIS.length())
                + ELLIPSIS;
    }

    private static String ellipsizeWithSuffix(
            String value,
            String suffix,
            int maxChars
    ) {
        Objects.requireNonNull(suffix, "display suffix cannot be null");
        if (suffix.length() >= maxChars) {
            throw new IllegalArgumentException(
                    "display suffix is too long"
            );
        }

        int available = maxChars - suffix.length();
        return ellipsize(value, available) + suffix;
    }
}
