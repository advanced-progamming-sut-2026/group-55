package pvz.model.quest;

/** Display priority for Travel Log quests. Lower order values appear first. */
public enum QuestPriority {
    CRITICAL(0),
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int sortOrder;

    QuestPriority(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int sortOrder() {
        return sortOrder;
    }
}
