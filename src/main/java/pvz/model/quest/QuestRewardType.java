package pvz.model.quest;

/** Concrete rewards supported by the current game model. */
public enum QuestRewardType {
    COINS(QuestRewardCategory.CURRENCY, false),
    DIAMONDS(QuestRewardCategory.CURRENCY, false),
    PLANT_UNLOCK(QuestRewardCategory.UNLOCKABLE, true),
    LEVEL_UNLOCK(QuestRewardCategory.UNLOCKABLE, true),
    SEED_PACKETS(QuestRewardCategory.INVENTORY, true);

    private final QuestRewardCategory category;
    private final boolean targetRequired;

    QuestRewardType(
            QuestRewardCategory category,
            boolean targetRequired
    ) {
        this.category = category;
        this.targetRequired = targetRequired;
    }

    public QuestRewardCategory category() {
        return category;
    }

    public boolean targetRequired() {
        return targetRequired;
    }
}
