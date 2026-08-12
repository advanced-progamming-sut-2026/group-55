package pvz.model.entity.plant.shooter.plantfood;

import java.util.List;
import java.util.Objects;

import pvz.model.entity.projectile.ProjectileType;

public record ShooterPlantFoodProfile(
        List<ShooterPlantFoodPhase> phases,
        boolean targetsMatchingPlantsOnBoard
) {
    public ShooterPlantFoodProfile {
        phases = List.copyOf(
                Objects.requireNonNull(
                        phases,
                        "shooter plant food phases cannot be null"
                )
        );

        if (phases.isEmpty()) {
            throw new IllegalArgumentException(
                    "shooter plant food needs at least one phase"
            );
        }
    }

    public ShooterPlantFoodProfile(
            List<ShooterPlantFoodPhase> phases
    ) {
        this(phases, false);
    }

    public ShooterPlantFoodProfile(
            int durationTicks,
            long ticksBetweenSteps,
            double damagePerProjectile,
            List<PlantFoodShotPath> shotPaths,
            ProjectileType projectileType,
            int rangeTiles
    ) {
        this(
                List.of(
                        new ShooterPlantFoodPhase(
                                0,
                                durationTicks,
                                ticksBetweenSteps,
                                damagePerProjectile,
                                shotPaths,
                                projectileType,
                                rangeTiles
                        )
                ),
                false
        );
    }

    public int durationTicks() {
        return phases.stream()
                .mapToInt(ShooterPlantFoodPhase::endTickOffset)
                .max()
                .orElseThrow();
    }

    public long ticksBetweenSteps() {
        return primaryPhase().ticksBetweenSteps();
    }

    public double damagePerProjectile() {
        return primaryPhase().damagePerProjectile();
    }

    public List<PlantFoodShotPath> shotPaths() {
        return primaryPhase().shotPaths();
    }

    public ProjectileType projectileType() {
        return primaryPhase().projectileType();
    }

    public int rangeTiles() {
        return primaryPhase().rangeTiles();
    }

    public int stepCount() {
        return primaryPhase().stepCount();
    }

    public int stepCount(long effectiveDurationTicks) {
        if (phases.size() == 1) {
            return primaryPhase().stepCount(
                    effectiveDurationTicks
            );
        }

        if (effectiveDurationTicks < durationTicks()) {
            throw new IllegalArgumentException(
                    "effective plant food duration is shorter than its phases"
            );
        }

        return primaryPhase().stepCount();
    }

    public int shotsAtStep(
            PlantFoodShotPath path,
            int stepIndex
    ) {
        return shotsAtStep(
                path,
                stepIndex,
                stepCount()
        );
    }

    public int shotsAtStep(
            PlantFoodShotPath path,
            int stepIndex,
            int totalSteps
    ) {
        return primaryPhase().shotsAtStep(
                path,
                stepIndex,
                totalSteps
        );
    }

    private ShooterPlantFoodPhase primaryPhase() {
        return phases.getFirst();
    }
}
