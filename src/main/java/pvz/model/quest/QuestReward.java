package pvz.model.quest;

import java.util.Objects;

/** Immutable description of one reward granted when a quest is claimed. */
public record QuestReward(
        QuestRewardType type,
        int amount,
        String targetId
) {
    public QuestReward {
        Objects.requireNonNull(type, "quest reward type cannot be null");
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "quest reward amount must be positive"
            );
        }

        targetId = normalizeOptional(targetId);
        if (type.targetRequired() && targetId == null) {
            throw new IllegalArgumentException(
                    "quest reward target is required for " + type
            );
        }
        if (!type.targetRequired() && targetId != null) {
            throw new IllegalArgumentException(
                    "quest reward target is not allowed for " + type
            );
        }
        if ((type == QuestRewardType.PLANT_UNLOCK
                || type == QuestRewardType.LEVEL_UNLOCK)
                && amount != 1) {
            throw new IllegalArgumentException(
                    "unlock rewards must have amount 1"
            );
        }
    }

    public QuestRewardCategory category() {
        return type.category();
    }

    public static QuestReward coins(int amount) {
        return new QuestReward(QuestRewardType.COINS, amount, null);
    }

    public static QuestReward diamonds(int amount) {
        return new QuestReward(QuestRewardType.DIAMONDS, amount, null);
    }

    public static QuestReward plantUnlock(String plantName) {
        return new QuestReward(
                QuestRewardType.PLANT_UNLOCK,
                1,
                requireTarget(plantName, "plant name")
        );
    }

    public static QuestReward levelUnlock(String levelId) {
        return new QuestReward(
                QuestRewardType.LEVEL_UNLOCK,
                1,
                requireTarget(levelId, "level id")
        );
    }

    public static QuestReward seedPackets(
            String plantName,
            int amount
    ) {
        return new QuestReward(
                QuestRewardType.SEED_PACKETS,
                amount,
                requireTarget(plantName, "plant name")
        );
    }

    private static String requireTarget(String value, String field) {
        Objects.requireNonNull(value, field + " cannot be null");
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}
