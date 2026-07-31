package pvz.model.entity.plant.shooter;

import java.util.Objects;

import pvz.model.core.HorizontalDirection;

public record PlantFoodShotPath(
        int laneOffset,
        HorizontalDirection direction,
        int totalShots
) {
    public PlantFoodShotPath {
        Objects.requireNonNull(direction, "shot direction cannot be null");

        if (totalShots <= 0) {
            throw new IllegalArgumentException("total shots must be positive");
        }
    }
}
