package pvz.model.leaderboard;

import java.util.Objects;

/**
 * Immutable sort state shared by the graphical leaderboard and future clients.
 *
 * <p>When the user selects the already-active column, the direction toggles.
 * Selecting a new column uses a sensible leaderboard default: usernames start
 * ascending while progress/statistic columns start descending.</p>
 */
public record LeaderboardSort(
        LeaderboardSortKey key,
        SortDirection direction
) {
    public LeaderboardSort {
        key = Objects.requireNonNull(key, "sort key cannot be null");
        direction = Objects.requireNonNull(
                direction,
                "sort direction cannot be null"
        );
    }

    public static LeaderboardSort defaultSort() {
        return forColumn(LeaderboardSortKey.ADVENTURE_PROGRESS);
    }

    public static LeaderboardSort forColumn(LeaderboardSortKey key) {
        Objects.requireNonNull(key, "sort key cannot be null");
        SortDirection direction = key == LeaderboardSortKey.USERNAME
                ? SortDirection.ASCENDING
                : SortDirection.DESCENDING;
        return new LeaderboardSort(key, direction);
    }

    public LeaderboardSort select(LeaderboardSortKey selectedKey) {
        Objects.requireNonNull(selectedKey, "selected sort key cannot be null");
        if (selectedKey == key) {
            return new LeaderboardSort(key, direction.toggled());
        }
        return forColumn(selectedKey);
    }
}
