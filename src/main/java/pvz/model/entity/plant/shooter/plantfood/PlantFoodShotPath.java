package pvz.model.entity.plant.shooter.plantfood;

import java.util.Objects;

import pvz.model.core.board.HorizontalDirection;
import pvz.model.entity.plant.attack.ShotVector;

public record PlantFoodShotPath(
        int laneOffset,
        ShotVector vector,
        int totalShots
) {
    public PlantFoodShotPath {
        vector = Objects.requireNonNull(
                vector,
                "shot vector cannot be null"
        );

        if (totalShots <= 0) {
            throw new IllegalArgumentException(
                    "total shots must be positive"
            );
        }
    }

    public PlantFoodShotPath(
            int laneOffset,
            HorizontalDirection direction,
            int totalShots
    ) {
        this(
                laneOffset,
                toShotVector(direction),
                totalShots
        );
    }

    private static ShotVector toShotVector(
            HorizontalDirection direction
    ) {
        Objects.requireNonNull(
                direction,
                "shot direction cannot be null"
        );

        return direction == HorizontalDirection.RIGHT
                ? ShotVector.RIGHT
                : ShotVector.LEFT;
    }
}
