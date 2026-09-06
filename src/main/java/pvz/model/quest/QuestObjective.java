package pvz.model.quest;

import java.util.Objects;

/** A measurable completion condition for a quest. */
public record QuestObjective(
        QuestMetric metric,
        int target,
        String subjectId
) {
    public QuestObjective {
        Objects.requireNonNull(metric, "quest metric cannot be null");
        if (target <= 0) {
            throw new IllegalArgumentException(
                    "quest target must be positive"
            );
        }
        subjectId = normalizeOptional(subjectId);
    }

    public static QuestObjective global(
            QuestMetric metric,
            int target
    ) {
        return new QuestObjective(metric, target, null);
    }

    public static QuestObjective forSubject(
            QuestMetric metric,
            String subjectId,
            int target
    ) {
        Objects.requireNonNull(subjectId, "subject id cannot be null");
        if (subjectId.isBlank()) {
            throw new IllegalArgumentException(
                    "subject id cannot be blank"
            );
        }
        return new QuestObjective(metric, target, subjectId);
    }

    public boolean hasSubject() {
        return subjectId != null;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}
