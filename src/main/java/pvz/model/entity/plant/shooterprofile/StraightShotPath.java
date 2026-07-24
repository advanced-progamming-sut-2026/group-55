package pvz.model.entity.plant.shooterprofile;

import java.util.Objects;
import pvz.model.core.HorizontalDirection;

public record StraightShotPath(
        int laneOffset,
        HorizontalDirection direction,
        int shotsPerVolley
) {
    public StraightShotPath {
        Objects.requireNonNull(direction, "shot direction cannot be null");

        if (shotsPerVolley <= 0) {
            throw new IllegalArgumentException("shots per volley must be positive");
        }
    }
}