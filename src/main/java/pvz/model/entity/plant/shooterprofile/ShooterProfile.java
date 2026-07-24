package pvz.model.entity.plant.shooterprofile;

import java.util.List;
import java.util.Objects;
import pvz.model.entity.projectile.ProjectileType;

public record ShooterProfile(
        double damagePerProjectile,
        long ticksBetweenShots,
        List<StraightShotPath> shotPaths,
        ProjectileType projectileType,
        int rangeTiles
) {
    public ShooterProfile {
        if (damagePerProjectile < 0) {
            throw new IllegalArgumentException("projectile damage cannot be negative");
        }

        if (ticksBetweenShots < 0) {
            throw new IllegalArgumentException("ticks between shots cannot be negative");
        }

        shotPaths = List.copyOf(
                Objects.requireNonNull(shotPaths, "shot paths cannot be null")
        );

        if (shotPaths.isEmpty()) {
            throw new IllegalArgumentException("shooter needs at least one shot path");
        }

        int burstLength = shotPaths.stream()
                .mapToInt(StraightShotPath::shotsPerVolley).max().orElseThrow();

        if (burstLength > 1 && ticksBetweenShots == 0) {
            throw new IllegalArgumentException("multi-shot profiles need a positive shot gap");
        }

        projectileType = Objects.requireNonNull(projectileType,
                "projectile type cannot be null"
        );

        if (rangeTiles <= 0) {
            throw new IllegalArgumentException("shooter range must be positive");
        }
    }

    public int burstLength() {
        return shotPaths.stream()
                .mapToInt(StraightShotPath::shotsPerVolley)
                .max()
                .orElseThrow();
    }
}