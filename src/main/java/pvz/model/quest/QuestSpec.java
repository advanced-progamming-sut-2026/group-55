package pvz.model.quest;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Static definition of a Travel Log quest. */
public record QuestSpec(
        String id,
        String name,
        String description,
        QuestCategory category,
        QuestPriority priority,
        QuestObjective objective,
        List<QuestReward> rewards,
        QuestResetPolicy resetPolicy,
        boolean initiallyAvailable
) {
    public QuestSpec {
        id = normalizeId(id);
        name = requireText(name, "quest name");
        description = requireText(description, "quest description");
        Objects.requireNonNull(category, "quest category cannot be null");
        Objects.requireNonNull(priority, "quest priority cannot be null");
        Objects.requireNonNull(objective, "quest objective cannot be null");
        Objects.requireNonNull(rewards, "quest rewards cannot be null");
        if (rewards.isEmpty()) {
            throw new IllegalArgumentException(
                    "quest must have at least one reward"
            );
        }
        if (rewards.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "quest rewards cannot contain null"
            );
        }
        rewards = List.copyOf(rewards);
        Objects.requireNonNull(
                resetPolicy,
                "quest reset policy cannot be null"
        );
    }

    /** Backwards-compatible constructor for quests available immediately. */
    public QuestSpec(
            String id,
            String name,
            String description,
            QuestCategory category,
            QuestPriority priority,
            QuestObjective objective,
            List<QuestReward> rewards,
            QuestResetPolicy resetPolicy
    ) {
        this(
                id,
                name,
                description,
                category,
                priority,
                objective,
                rewards,
                resetPolicy,
                true
        );
    }

    public static String normalizeId(String value) {
        return requireText(value, "quest id")
                .toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " cannot be null");
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return normalized;
    }
}
