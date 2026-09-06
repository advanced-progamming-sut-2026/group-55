package pvz.model.leaderboard;

import java.util.Objects;

/** Immutable row-shaped snapshot consumed by leaderboard presentation code. */
public record LeaderboardEntry(
        String username,
        String nickname,
        AdventureStanding adventure,
        int completedMinigameStages,
        int completedDailyQuests,
        int completedNonDailyQuests,
        int maxMewPoint
) {
    public LeaderboardEntry {
        username = requireText(username, "username");
        nickname = normalizeNickname(nickname, username);
        adventure = Objects.requireNonNull(
                adventure,
                "adventure standing cannot be null"
        );
        requireNonNegative(
                completedMinigameStages,
                "completed minigame stages"
        );
        requireNonNegative(
                completedDailyQuests,
                "completed daily quests"
        );
        requireNonNegative(
                completedNonDailyQuests,
                "completed non-daily quests"
        );
        requireNonNegative(maxMewPoint, "max mew point");
    }

    private static String normalizeNickname(String value, String username) {
        if (value == null || value.isBlank()) {
            return username;
        }
        return value.strip();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " cannot be null");
        String checked = value.strip();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return checked;
    }

    private static void requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
    }
}
