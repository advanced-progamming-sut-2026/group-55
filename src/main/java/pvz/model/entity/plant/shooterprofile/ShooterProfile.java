package pvz.model.entity.plant.shooterprofile;

import java.util.List;
import java.util.Objects;
import pvz.model.entity.projectile.ProjectileType;

public record ShooterProfile(
        double damagePerProjectile,
        int shotsPerLane,
        long ticksBetweenShots,
        List<Integer> laneOffsets,
        ProjectileType projectileType,
        int rangeTiles
) {
    public ShooterProfile {
        if (damagePerProjectile < 0) {
            throw new IllegalArgumentException(
                    "projectile damage cannot be negative"
            );
        }

        if (shotsPerLane <= 0) {
            throw new IllegalArgumentException(
                    "shots per lane must be positive"
            );
        }

        if (ticksBetweenShots < 0) {
            throw new IllegalArgumentException(
                    "ticks between shots cannot be negative"
            );
        }

        if (shotsPerLane > 1 && ticksBetweenShots == 0) {
            throw new IllegalArgumentException(
                    "multi-shot profiles need a positive shot gap"
            );
        }

        Objects.requireNonNull(
                laneOffsets,
                "lane offsets cannot be null"
        );

        if (rangeTiles <= 0) {
            throw new IllegalArgumentException(
                    "shooter range must be positive"
            );
        }

        if (laneOffsets.isEmpty()) {
            throw new IllegalArgumentException(
                    "lane offsets cannot be empty"
            );
        }

        laneOffsets = List.copyOf(laneOffsets);

        projectileType = Objects.requireNonNull(
                projectileType,
                "projectile type cannot be null"
        );
    }
}