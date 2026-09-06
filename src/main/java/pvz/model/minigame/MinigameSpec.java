package pvz.model.minigame;

import java.util.Locale;
import java.util.Objects;

/** Immutable definition of a Travel Log minigame entry. */
public record MinigameSpec(
        String id,
        String name,
        String description,
        int stageCount
) {
    public MinigameSpec {
        id = normalizeId(id);
        name = requireText(name, "minigame name");
        description = requireText(description, "minigame description");
        if (stageCount <= 0) {
            throw new IllegalArgumentException(
                    "minigame stage count must be positive"
            );
        }
    }

    public boolean hasStage(int stageNumber) {
        return stageNumber >= 1 && stageNumber <= stageCount;
    }

    public String stageId(int stageNumber) {
        if (!hasStage(stageNumber)) {
            throw new IllegalArgumentException(
                    "invalid stage " + stageNumber + " for " + id
            );
        }
        return id + "-" + stageNumber;
    }

    public static String normalizeId(String value) {
        String result = requireText(value, "minigame id")
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-');
        while (result.contains("--")) {
            result = result.replace("--", "-");
        }
        return result;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " cannot be null");
        String result = value.strip();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return result;
    }
}
