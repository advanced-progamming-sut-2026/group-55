package pvz.model.adventure;

import java.util.Objects;

public record LevelSpec(
        String id,
        String chapterId,
        int number,
        String name,
        LevelType type,
        int columns,
        int rows,
        int startingSun,
        boolean skySunEnabled,
        ObjectiveType objectiveType
) {
    public LevelSpec {
        id = requireText(id, "level id");
        chapterId = requireText(chapterId, "chapter id");
        name = requireText(name, "level name");
        Objects.requireNonNull(type, "level type cannot be null");
        Objects.requireNonNull(
                objectiveType,
                "objective type cannot be null"
        );
        if (number <= 0) {
            throw new IllegalArgumentException(
                    "level number must be positive"
            );
        }
        if (columns <= 0 || columns > 9) {
            throw new IllegalArgumentException(
                    "level columns must be between 1 and 9"
            );
        }
        if (rows <= 0 || rows > 5) {
            throw new IllegalArgumentException(
                    "level rows must be between 1 and 5"
            );
        }
        if (startingSun < 0) {
            throw new IllegalArgumentException(
                    "starting sun cannot be negative"
            );
        }
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
