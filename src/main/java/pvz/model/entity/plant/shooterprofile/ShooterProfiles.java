package pvz.model.entity.plant.shooterprofile;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.projectile.ProjectileType;

public final class ShooterProfiles {
    private static final double DEFAULT_SHOT_DAMAGE = 20;
    private static final long RAPID_SHOT_GAP_TICKS = 2;
    private static final int FULL_BOARD_RANGE = Integer.MAX_VALUE;
    private static final int SHORT_RANGE_TILES = 3;

    private ShooterProfiles() {
    }

    public static ShooterProfile from(PlantSpec spec) {
        Objects.requireNonNull(
                spec,
                "plant spec cannot be null"
        );

        if (spec.getCategory() != PlantCategory.SHOOTER) {
            throw new IllegalArgumentException(
                    spec.getName() + " is not a shooter"
            );
        }

        return switch (spec.getName().toLowerCase(Locale.ROOT)) {
            case "peashooter" -> singleLaneProfile(
                    20,
                    1,
                    0,
                    ProjectileType.NORMAL
            );

            case "repeater" -> singleLaneProfile(
                    20,
                    2,
                    RAPID_SHOT_GAP_TICKS,
                    ProjectileType.NORMAL
            );

            case "threepeater" -> new ShooterProfile(
                    20,
                    1,
                    0,
                    List.of(-1, 0, 1),
                    ProjectileType.NORMAL,
                    FULL_BOARD_RANGE
            );

            case "fire peashooter" -> singleLaneProfile(
                    20,
                    1,
                    0,
                    ProjectileType.FIRE
            );

            case "mega gatling pea" -> singleLaneProfile(
                    20,
                    4,
                    RAPID_SHOT_GAP_TICKS,
                    ProjectileType.NORMAL
            );

            case "puff-shroom", "sea-shroom" -> singleLaneProfile(
                            20,
                            1,
                            0,
                            ProjectileType.NORMAL,
                            SHORT_RANGE_TILES
                    );

            default -> createFallbackProfile(spec);
        };
    }

    private static ShooterProfile singleLaneProfile(
            double damage,
            int shotsPerLane,
            long ticksBetweenShots,
            ProjectileType projectileType
    ) {
        return singleLaneProfile(
                damage,
                shotsPerLane,
                ticksBetweenShots,
                projectileType,
                FULL_BOARD_RANGE
        );
    }
    private static ShooterProfile singleLaneProfile(
            double damage,
            int shotsPerLane,
            long ticksBetweenShots,
            ProjectileType projectileType,
            int rangeTiles
    ) {
        return new ShooterProfile(
                damage,
                shotsPerLane,
                ticksBetweenShots,
                List.of(0),
                projectileType,
                rangeTiles
        );
    }

    private static ShooterProfile createFallbackProfile(
            PlantSpec spec
    ) {
        return singleLaneProfile(
                parseSimpleDamage(spec.getDamage()),
                1,
                0,
                ProjectileType.NORMAL
        );
    }

    private static double parseSimpleDamage(String damageText) {
        try {
            return Double.parseDouble(damageText);
        } catch (NumberFormatException exception) {
            return DEFAULT_SHOT_DAMAGE;
        }
    }
}