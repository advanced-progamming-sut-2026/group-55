package pvz.model.leaderboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Application-facing access point for leaderboard snapshots and ordering. */
public final class LeaderboardService {
    private final LeaderboardDataSource dataSource;

    public LeaderboardService(LeaderboardDataSource dataSource) {
        this.dataSource = Objects.requireNonNull(
                dataSource,
                "leaderboard data source cannot be null"
        );
    }

    /**
     * Returns the source order as a defensive immutable snapshot.
     *
     * <p>This method is kept for consumers that need raw source order. The
     * graphical leaderboard should use {@link #snapshot(LeaderboardSort)}.</p>
     */
    public List<LeaderboardEntry> snapshot() {
        return List.copyOf(validatedEntries());
    }

    /** Returns an immutable snapshot sorted with the supplied leaderboard state. */
    public List<LeaderboardEntry> snapshot(LeaderboardSort sort) {
        Objects.requireNonNull(sort, "leaderboard sort cannot be null");
        List<LeaderboardEntry> sorted = new ArrayList<>(validatedEntries());
        sorted.sort(LeaderboardOrdering.comparator(sort));
        return List.copyOf(sorted);
    }

    /** Convenience overload for non-UI consumers. */
    public List<LeaderboardEntry> snapshot(
            LeaderboardSortKey key,
            SortDirection direction
    ) {
        return snapshot(new LeaderboardSort(key, direction));
    }

    private List<LeaderboardEntry> validatedEntries() {
        List<LeaderboardEntry> loaded = Objects.requireNonNull(
                dataSource.loadEntries(),
                "leaderboard data source returned null"
        );
        for (LeaderboardEntry entry : loaded) {
            Objects.requireNonNull(
                    entry,
                    "leaderboard data source returned a null entry"
            );
        }
        return loaded;
    }
}
