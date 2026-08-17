package pvz.model.entity.plant.category.shooter;

import java.util.List;
import java.util.Objects;

import pvz.model.entity.plant.attack.ProjectileAttackProfile;
import pvz.model.entity.plant.attack.ShotPath;
import pvz.model.entity.projectile.ProjectileType;

public record ShooterProfile(
        double damagePerProjectile,
        long ticksBetweenShots,
        List<ShotPath> shotPaths,
        ProjectileType projectileType,
        int rangeTiles
) implements ProjectileAttackProfile {
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
                .mapToInt(ShotPath::shotsPerVolley)
                .max()
                .orElseThrow();

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

}
