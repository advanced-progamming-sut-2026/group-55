package pvz.model.leaderboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeaderboardServiceTest {
    @Test
    void snapshotIsDefensiveAndImmutable() {
        List<LeaderboardEntry> mutable = new ArrayList<>();
        mutable.add(entry("alpha"));
        LeaderboardService service = new LeaderboardService(() -> mutable);

        List<LeaderboardEntry> snapshot = service.snapshot();
        mutable.add(entry("beta"));

        assertEquals(1, snapshot.size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.add(entry("gamma"))
        );
    }

    @Test
    void rejectsBrokenDataSourceContracts() {
        LeaderboardService nullList = new LeaderboardService(() -> null);
        assertThrows(NullPointerException.class, nullList::snapshot);

        LeaderboardService nullEntry = new LeaderboardService(
                () -> java.util.Arrays.asList(entry("alpha"), null)
        );
        assertThrows(NullPointerException.class, nullEntry::snapshot);
    }

    @Test
    void sortedSnapshotIsImmutableAndRejectsNullSort() {
        LeaderboardService service = new LeaderboardService(() -> List.of(
                entry("beta"),
                entry("alpha")
        ));

        List<LeaderboardEntry> sorted = service.snapshot(
                LeaderboardSortKey.USERNAME,
                SortDirection.ASCENDING
        );
        assertEquals("alpha", sorted.get(0).username());
        assertThrows(
                UnsupportedOperationException.class,
                () -> sorted.add(entry("gamma"))
        );
        assertThrows(NullPointerException.class, () -> service.snapshot(null));
    }

    private LeaderboardEntry entry(String username) {
        return new LeaderboardEntry(
                username,
                username,
                AdventureStanding.none(),
                0,
                0,
                0,
                0
        );
    }
}
