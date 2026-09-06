package pvz.model.adventure;

import java.util.Objects;

/** Read-only player-facing objective copy for the level-intro UI. */
public record LevelObjectivePresentation(
        String title,
        String description
) {
    public LevelObjectivePresentation {
        title = requireText(title, "objective title");
        description = requireText(description, "objective description");
    }

    public static LevelObjectivePresentation forObjective(ObjectiveType objectiveType) {
        Objects.requireNonNull(objectiveType, "objective type cannot be null");
        return switch (objectiveType) {
            case CLEAR_ALL_WAVES -> new LevelObjectivePresentation(
                    "DEFEND THE HOUSE",
                    "Defeat every zombie wave. Do not let a zombie reach the house."
            );
        };
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        String stripped = value.strip();
        if (stripped.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return stripped;
    }
}
