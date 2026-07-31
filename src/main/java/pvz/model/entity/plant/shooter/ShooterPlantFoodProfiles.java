package pvz.model.entity.plant.shooter;

import java.util.List;
import java.util.Locale;

import pvz.model.core.Game;
import pvz.model.entity.plant.PlantSpec;

public final class ShooterPlantFoodProfiles {

    private static final int PEASHOOTER_DURATION_TICKS = 2 * Game.TICKS_PER_SECOND;
    private static final int PEASHOOTER_TOTAL_SHOTS = 60;
    private static final long RAPID_VOLLEY_STEP_GAP_TICKS = 1;

    private ShooterPlantFoodProfiles() {
    }

    public static ShooterPlantFoodProfile from(PlantSpec spec) {
        if (spec == null) {
            return null;
        }

        String plantName = spec.getName()
                .strip()
                .toLowerCase(Locale.ROOT);

        return switch (plantName) {
            case "peashooter" -> createPeashooterProfile(spec);
            default -> null;
        };
    }

    private static ShooterPlantFoodProfile createPeashooterProfile(
            PlantSpec spec
    ) {
        ShooterProfile baseProfile = ShooterProfiles.from(spec);
        StraightShotPath basePath = baseProfile.shotPaths().getFirst();

        return new ShooterPlantFoodProfile(
                PEASHOOTER_DURATION_TICKS,
                RAPID_VOLLEY_STEP_GAP_TICKS,
                baseProfile.damagePerProjectile(),
                List.of(
                        new PlantFoodShotPath(
                                basePath.laneOffset(),
                                basePath.direction(),
                                PEASHOOTER_TOTAL_SHOTS
                        )
                ),
                baseProfile.projectileType(),
                baseProfile.rangeTiles()
        );
    }
}
