package pvz.model.entity.plant.level;

import java.util.Objects;

public record PlantLevelUpgrade(
        int targetLevel,
        PlantUpgradeType type,
        double value,
        String sourceText
) {
    public PlantLevelUpgrade {
        if (targetLevel < 2 || targetLevel > 4) {
            throw new IllegalArgumentException("targetLevel must be between 2 and 4");
        }
        Objects.requireNonNull(type, "type cannot be null");
        sourceText = Objects.requireNonNull(sourceText, "sourceText cannot be null").strip();
        if (sourceText.isEmpty()) {
            throw new IllegalArgumentException("sourceText cannot be blank");
        }
    }
}
