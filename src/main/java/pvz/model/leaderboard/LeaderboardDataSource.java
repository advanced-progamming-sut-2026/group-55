package pvz.model.leaderboard;

import java.util.List;

/**
 * Boundary between leaderboard consumers and where leaderboard rows come from.
 *
 * <p>Phase 2 uses a local implementation backed by UserManager. Phase 3 can
 * replace that implementation with a server-backed source without changing
 * LeaderboardScreen or leaderboard sorting code.</p>
 */
@FunctionalInterface
public interface LeaderboardDataSource {
    List<LeaderboardEntry> loadEntries();
}
