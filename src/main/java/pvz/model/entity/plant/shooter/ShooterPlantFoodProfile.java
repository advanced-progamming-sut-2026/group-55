package pvz.model.entity.plant.shooter;

import java.util.List;
import java.util.Objects;

import pvz.model.entity.projectile.ProjectileType;

public record ShooterPlantFoodProfile(
        int durationTicks,
        long ticksBetweenSteps,
        double damagePerProjectile,
        List<PlantFoodShotPath> shotPaths,
        ProjectileType projectileType,
        int rangeTiles
) {
    public ShooterPlantFoodProfile {
        if (durationTicks <= 0) {
            throw new IllegalArgumentException(
                    "plant food duration must be positive"
            );
        }

        if (ticksBetweenSteps <= 0) {
            throw new IllegalArgumentException(
                    "ticks between plant food steps must be positive"
            );
        }

        if (damagePerProjectile < 0) {
            throw new IllegalArgumentException(
                    "projectile damage cannot be negative"
            );
        }

        shotPaths = List.copyOf(
                Objects.requireNonNull(
                        shotPaths,
                        "plant food shot paths cannot be null"
                )
        );

        if (shotPaths.isEmpty()) {
            throw new IllegalArgumentException(
                    "shooter plant food needs at least one shot path"
            );
        }

        projectileType = Objects.requireNonNull(
                projectileType,
                "projectile type cannot be null"
        );

        if (rangeTiles <= 0) {
            throw new IllegalArgumentException(
                    "shooter plant food range must be positive"
            );
        }
    }

    public int stepCount() {
        return stepCount(durationTicks);
    }

    public int stepCount(long effectiveDurationTicks) {
        if (effectiveDurationTicks <= 0) {
            throw new IllegalArgumentException(
                    "effective plant food duration must be positive"
            );
        }

        long steps = (effectiveDurationTicks + ticksBetweenSteps - 1) / ticksBetweenSteps;

        return Math.toIntExact(steps);// long -> int
    }

    public int shotsAtStep(PlantFoodShotPath path, int stepIndex) {
        return shotsAtStep(
                path,
                stepIndex,
                stepCount()
        );
    }

    public int shotsAtStep(PlantFoodShotPath path, int stepIndex, int totalSteps) {
        Objects.requireNonNull(
                path,
                "plant food shot path cannot be null"
        );

        if (totalSteps <= 0) {
            throw new IllegalArgumentException(
                    "total plant food steps must be positive"
            );
        }

        if (stepIndex < 0 || stepIndex >= totalSteps) {
            throw new IllegalArgumentException(
                    "plant food step index is out of range"
            );
        }

        int shotsBefore = (int) ((long) stepIndex * path.totalShots() / totalSteps);

        int shotsAfter = (int) ((long) (stepIndex + 1) * path.totalShots() / totalSteps);

        return shotsAfter - shotsBefore;
    }
}
