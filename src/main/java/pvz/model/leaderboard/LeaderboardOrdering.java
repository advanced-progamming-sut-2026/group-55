package pvz.model.leaderboard;

import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/** Centralized, deterministic ordering rules for every leaderboard column. */
final class LeaderboardOrdering {
    private static final Comparator<LeaderboardEntry> USERNAME_ORDER =
            Comparator.comparing(
                            (LeaderboardEntry entry) ->
                                    entry.username().toLowerCase(Locale.ROOT)
                    )
                    .thenComparing(LeaderboardEntry::username);

    private LeaderboardOrdering() {
    }

    static Comparator<LeaderboardEntry> comparator(LeaderboardSort sort) {
        Objects.requireNonNull(sort, "leaderboard sort cannot be null");

        Comparator<LeaderboardEntry> primary = switch (sort.key()) {
            case USERNAME -> USERNAME_ORDER;
            case ADVENTURE_PROGRESS -> Comparator
                    .comparingInt((LeaderboardEntry entry) ->
                            entry.adventure().chapterOrder())
                    .thenComparingInt(entry ->
                            entry.adventure().levelNumber());
            case MINIGAME_COMPLETIONS -> Comparator.comparingInt(
                    LeaderboardEntry::completedMinigameStages
            );
            case DAILY_QUEST_COMPLETIONS -> Comparator.comparingInt(
                    LeaderboardEntry::completedDailyQuests
            );
            case NON_DAILY_QUEST_COMPLETIONS -> Comparator.comparingInt(
                    LeaderboardEntry::completedNonDailyQuests
            );
            case MAX_MEW_POINT -> Comparator.comparingInt(
                    LeaderboardEntry::maxMewPoint
            );
        };

        if (sort.direction() == SortDirection.DESCENDING) {
            primary = primary.reversed();
        }

        // Ties always resolve by username ascending so ordering is stable across
        // local saves, different JVM map orders and the future server source.
        if (sort.key() == LeaderboardSortKey.USERNAME) {
            return primary;
        }
        return primary.thenComparing(USERNAME_ORDER);
    }
}
