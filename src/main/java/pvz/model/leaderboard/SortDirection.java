package pvz.model.leaderboard;

/** Sort direction shared by console, graphical and future server-backed UI. */
public enum SortDirection {
    ASCENDING,
    DESCENDING;

    public SortDirection toggled() {
        return this == ASCENDING ? DESCENDING : ASCENDING;
    }
}
