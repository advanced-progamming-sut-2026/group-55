package pvz.model.quest;

import java.util.Objects;

/**
 * Phase-boundary event consumed by the quest system later.
 *
 * <p>Adventure, battle and minigame code can publish these values without
 * depending on Travel Log screens or quest services.</p>
 */
public record QuestEvent(
        QuestMetric metric,
        int amount,
        String subjectId
) {
    public QuestEvent {
        Objects.requireNonNull(metric, "quest event metric cannot be null");
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "quest event amount must be positive"
            );
        }
        subjectId = normalizeOptional(subjectId);
    }

    public static QuestEvent battleCompleted() {
        return new QuestEvent(QuestMetric.BATTLE_COMPLETED, 1, null);
    }

    public static QuestEvent levelCompleted(String levelId) {
        return subjectEvent(QuestMetric.LEVEL_COMPLETED, levelId, 1);
    }

    public static QuestEvent chapterCompleted(String chapterId) {
        return subjectEvent(QuestMetric.CHAPTER_COMPLETED, chapterId, 1);
    }

    public static QuestEvent zombieKilled(String zombieId) {
        return subjectEvent(QuestMetric.ZOMBIE_KILLED, zombieId, 1);
    }

    public static QuestEvent plantPlaced(String plantName) {
        return subjectEvent(QuestMetric.PLANT_PLACED, plantName, 1);
    }

    public static QuestEvent plantUpgraded(String plantName) {
        return subjectEvent(QuestMetric.PLANT_UPGRADED, plantName, 1);
    }

    public static QuestEvent sunSpent(int amount) {
        return new QuestEvent(QuestMetric.SUN_SPENT, amount, null);
    }

    public static QuestEvent coinsEarned(int amount) {
        return new QuestEvent(QuestMetric.COINS_EARNED, amount, null);
    }

    public static QuestEvent diamondsEarned(int amount) {
        return new QuestEvent(QuestMetric.DIAMONDS_EARNED, amount, null);
    }

    public static QuestEvent seedPacketsCollected(
            String plantName,
            int amount
    ) {
        return subjectEvent(
                QuestMetric.SEED_PACKETS_COLLECTED,
                plantName,
                amount
        );
    }

    public static QuestEvent minigameCompleted(String minigameId) {
        return subjectEvent(
                QuestMetric.MINIGAME_COMPLETED,
                minigameId,
                1
        );
    }

    private static QuestEvent subjectEvent(
            QuestMetric metric,
            String subjectId,
            int amount
    ) {
        Objects.requireNonNull(subjectId, "subject id cannot be null");
        if (subjectId.isBlank()) {
            throw new IllegalArgumentException(
                    "subject id cannot be blank"
            );
        }
        return new QuestEvent(metric, amount, subjectId);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}
