package pvz.model.entity.plant.shooter;

import java.util.Objects;

public record ShotPath(
        int laneOffset,
        ShotVector vector,
        int shotsPerVolley
) {
    public ShotPath {
        vector = Objects.requireNonNull(
                vector,
                "shot vector cannot be null"
        );

        if (shotsPerVolley <= 0) {
            throw new IllegalArgumentException(
                    "shots per volley must be positive"
            );
        }
    }
}
