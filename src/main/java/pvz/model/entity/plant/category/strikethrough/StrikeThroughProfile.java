package pvz.model.entity.plant.category.strikethrough;

import java.util.List;
import java.util.Objects;

import pvz.model.entity.plant.attack.ProjectileAttackProfile;
import pvz.model.entity.plant.attack.ShotPath;
import pvz.model.entity.projectile.ProjectileHitLimit;
import pvz.model.entity.projectile.ProjectileType;

public record StrikeThroughProfile(
        double damagePerProjectile,
        long ticksBetweenShots,
        List<ShotPath> shotPaths,
        ProjectileType projectileType,
        int rangeTiles,
        ProjectileHitLimit hitLimit
) implements ProjectileAttackProfile {

    public StrikeThroughProfile {
        if (damagePerProjectile < 0) {
            throw new IllegalArgumentException(
                    "projectile damage cannot be negative"
            );
        }

        if (ticksBetweenShots < 0) {
            throw new IllegalArgumentException(
                    "ticks between shots cannot be negative"
            );
        }

        shotPaths = List.copyOf(
                Objects.requireNonNull(
                        shotPaths,
                        "shot paths cannot be null"
                )
        );

        if (shotPaths.isEmpty()) {
            throw new IllegalArgumentException(
                    "strike-through attack needs at least one shot path"
            );
        }

        if (shotPaths.stream()
                .anyMatch(path -> !path.vector().isHorizontal())) {
            throw new IllegalArgumentException(
                    "strike-through paths must be horizontal"
            );
        }

        int burstLength = shotPaths.stream()
                .mapToInt(ShotPath::shotsPerVolley)
                .max()
                .orElseThrow();

        if (burstLength > 1 && ticksBetweenShots == 0) {
            throw new IllegalArgumentException(
                    "multi-shot profiles need a positive shot gap"
            );
        }

        projectileType = Objects.requireNonNull(
                projectileType,
                "projectile type cannot be null"
        );

        if (rangeTiles <= 0) {
            throw new IllegalArgumentException(
                    "strike-through range must be positive"
            );
        }

        hitLimit = Objects.requireNonNull(
                hitLimit,
                "projectile hit limit cannot be null"
        );
    }
}
