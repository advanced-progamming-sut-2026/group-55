package pvz.model.entity.plant.shooter.plantfood;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import pvz.model.core.Game;
import pvz.model.core.board.HorizontalDirection;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.PlantTag;
import pvz.model.entity.plant.attack.ShotPath;
import pvz.model.entity.plant.shooter.ShooterProfile;
import pvz.model.entity.plant.shooter.ShooterProfiles;
import pvz.model.entity.projectile.ProjectileHitLimit;

public final class ShooterPlantFoodProfiles {

    private static final int RAPID_VOLLEY_DURATION_TICKS =
            2 * Game.TICKS_PER_SECOND;
    private static final int GIANT_PROJECTILE_DURATION_TICKS = 4;
    private static final int RAPID_VOLLEY_TOTAL_SHOTS = 60;
    private static final int SHROOM_RAPID_VOLLEY_TOTAL_SHOTS = 30;
    private static final int CITRON_PLANT_FOOD_TOTAL_SHOTS = 1;
    private static final double CITRON_PLANT_FOOD_DAMAGE = 5000;
    private static final long RAPID_VOLLEY_STEP_GAP_TICKS = 1;
    private static final int FULL_BOARD_RANGE = Integer.MAX_VALUE;

    private static final Map<String, Integer>
            RAPID_VOLLEY_SHOTS_PER_PATH_OVERRIDES = Map.of(
                    "rotobaga",
                    180
            );

    private static final int PEA_POD_TOTAL_SHOTS = 5;
    private static final int MEGA_GATLING_GIANT_PEA_COUNT = 4;
    private static final double GIANT_PEA_DAMAGE_MULTIPLIER = 20;
    private static final int PEA_POD_DURATION_TICKS =
            PEA_POD_TOTAL_SHOTS
                    * GIANT_PROJECTILE_DURATION_TICKS;
    private static final long PEA_POD_VOLLEY_STEP_GAP_TICKS =
            GIANT_PROJECTILE_DURATION_TICKS;

    private ShooterPlantFoodProfiles() {
    }

    public static ShooterPlantFoodProfile from(PlantSpec spec) {
        if (spec == null
                || spec.getCategory() != PlantCategory.SHOOTER
                || spec.getName().equalsIgnoreCase("Appease-mint")) {
            return null;
        }

        if (spec.getTags().contains(PlantTag.SHROOM)) {
            return createShortLivedShroomProfile(spec);
        }

        String plantName = spec.getName()
                .strip()
                .toLowerCase(Locale.ROOT);

        Integer shotsPerPath =
                RAPID_VOLLEY_SHOTS_PER_PATH_OVERRIDES.get(plantName);

        if (shotsPerPath != null) {
            return createBasePathRapidVolleyProfile(
                    spec,
                    shotsPerPath
            );
        }

        return switch (plantName) {
            case "pea pod" -> createPeaPodProfile(spec);
            case "repeater" -> createRepeaterProfile(spec);
            case "mega gatling pea" ->
                    createMegaGatlingPeaProfile(spec);
            case "split pea" -> createSplitPeaProfile(spec);
            case "citron" -> createCitronProfile(spec);
            default -> createBasePathRapidVolleyProfile(
                    spec,
                    RAPID_VOLLEY_TOTAL_SHOTS
            );
        };
    }

    private static ShooterPlantFoodProfile createShortLivedShroomProfile(
            PlantSpec spec
    ) {
        ShooterProfile baseProfile = ShooterProfiles.from(spec);

        ShooterPlantFoodPhase rapidVolley =
                createRapidVolleyPhase(
                        baseProfile,
                        RAPID_VOLLEY_DURATION_TICKS,
                        plantFoodPathsFrom(
                                baseProfile,
                                SHROOM_RAPID_VOLLEY_TOTAL_SHOTS
                        ),
                        FULL_BOARD_RANGE
                );

        return new ShooterPlantFoodProfile(
                List.of(rapidVolley),
                true
        );
    }

    private static ShooterPlantFoodProfile
            createBasePathRapidVolleyProfile(
                    PlantSpec spec,
                    int totalShotsPerPath
            ) {
        ShooterProfile baseProfile = ShooterProfiles.from(spec);

        return new ShooterPlantFoodProfile(
                List.of(
                        createRapidVolleyPhase(
                                baseProfile,
                                plantFoodPathsFrom(
                                        baseProfile,
                                        totalShotsPerPath
                                )
                        )
                )
        );
    }

    private static ShooterPlantFoodProfile createRepeaterProfile(
            PlantSpec spec
    ) {
        ShooterProfile baseProfile = ShooterProfiles.from(spec);

        ShooterPlantFoodPhase rapidVolley =
                createRapidVolleyPhase(
                        baseProfile,
                        plantFoodPathsFrom(
                                baseProfile,
                                RAPID_VOLLEY_TOTAL_SHOTS
                        )
                );

        ShooterPlantFoodPhase giantPea = createGiantPeaPhase(
                baseProfile,
                RAPID_VOLLEY_DURATION_TICKS,
                1,
                HorizontalDirection.RIGHT
        );

        return new ShooterPlantFoodProfile(
                List.of(rapidVolley, giantPea)
        );
    }

    private static ShooterPlantFoodProfile createMegaGatlingPeaProfile(
            PlantSpec spec
    ) {
        ShooterProfile baseProfile = ShooterProfiles.from(spec);

        ShooterPlantFoodPhase rapidVolley =
                createRapidVolleyPhase(
                        baseProfile,
                        RAPID_VOLLEY_DURATION_TICKS * 2,
                        plantFoodPathsFrom(
                                baseProfile,
                                RAPID_VOLLEY_TOTAL_SHOTS * 2
                        )
                );

        ShooterPlantFoodPhase giantPeas = createGiantPeaPhase(
                baseProfile,
                RAPID_VOLLEY_DURATION_TICKS * 2,
                MEGA_GATLING_GIANT_PEA_COUNT,
                HorizontalDirection.RIGHT
        );

        return new ShooterPlantFoodProfile(
                List.of(rapidVolley, giantPeas)
        );
    }

    private static ShooterPlantFoodProfile createSplitPeaProfile(
            PlantSpec spec
    ) {
        ShooterProfile baseProfile = ShooterProfiles.from(spec);

        ShooterPlantFoodPhase simultaneousRapidVolleys =
                createRapidVolleyPhase(
                        baseProfile,
                        plantFoodPathsFrom(
                                baseProfile,
                                RAPID_VOLLEY_TOTAL_SHOTS
                        )
                );

        ShooterPlantFoodPhase rearGiantPea = createGiantPeaPhase(
                baseProfile,
                RAPID_VOLLEY_DURATION_TICKS,
                1,
                HorizontalDirection.LEFT
        );

        return new ShooterPlantFoodProfile(
                List.of(simultaneousRapidVolleys, rearGiantPea)
        );
    }


    private static ShooterPlantFoodProfile createCitronProfile(
            PlantSpec spec
    ) {
        ShooterProfile baseProfile = ShooterProfiles.from(spec);

        ShooterPlantFoodPhase laneClearingShot =
                new ShooterPlantFoodPhase(
                        0,
                        RAPID_VOLLEY_DURATION_TICKS,
                        RAPID_VOLLEY_DURATION_TICKS,
                        CITRON_PLANT_FOOD_DAMAGE,
                        plantFoodPathsFrom(
                                baseProfile,
                                CITRON_PLANT_FOOD_TOTAL_SHOTS
                        ),
                        baseProfile.projectileType(),
                        FULL_BOARD_RANGE,
                        ProjectileHitLimit.unlimited()
                );

        return new ShooterPlantFoodProfile(
                List.of(laneClearingShot)
        );
    }

    private static ShooterPlantFoodProfile createPeaPodProfile(
            PlantSpec spec
    ) {
        ShooterProfile baseProfile = ShooterProfiles.from(spec);
        ShotPath basePath = baseProfile.shotPaths().getFirst();

        return new ShooterPlantFoodProfile(
                PEA_POD_DURATION_TICKS,
                PEA_POD_VOLLEY_STEP_GAP_TICKS,
                baseProfile.damagePerProjectile()
                        * GIANT_PEA_DAMAGE_MULTIPLIER,
                List.of(
                        new PlantFoodShotPath(
                                basePath.laneOffset(),
                                basePath.vector(),
                                PEA_POD_TOTAL_SHOTS
                        )
                ),
                baseProfile.projectileType(),
                baseProfile.rangeTiles()
        );
    }

    private static List<PlantFoodShotPath> plantFoodPathsFrom(
            ShooterProfile baseProfile,
            int totalShotsPerPath
    ) {
        return baseProfile.shotPaths()
                .stream()
                .map(path -> new PlantFoodShotPath(
                        path.laneOffset(),
                        path.vector(),
                        totalShotsPerPath
                ))
                .toList();
    }

    private static ShooterPlantFoodPhase createRapidVolleyPhase(
            ShooterProfile baseProfile,
            List<PlantFoodShotPath> shotPaths
    ) {
        return createRapidVolleyPhase(
                baseProfile,
                RAPID_VOLLEY_DURATION_TICKS,
                shotPaths
        );
    }

    private static ShooterPlantFoodPhase createRapidVolleyPhase(
            ShooterProfile baseProfile,
            int durationTicks,
            List<PlantFoodShotPath> shotPaths
    ) {
        return createRapidVolleyPhase(
                baseProfile,
                durationTicks,
                shotPaths,
                baseProfile.rangeTiles()
        );
    }

    private static ShooterPlantFoodPhase createRapidVolleyPhase(
            ShooterProfile baseProfile,
            int durationTicks,
            List<PlantFoodShotPath> shotPaths,
            int rangeTiles
    ) {
        return new ShooterPlantFoodPhase(
                0,
                durationTicks,
                RAPID_VOLLEY_STEP_GAP_TICKS,
                baseProfile.damagePerProjectile(),
                shotPaths,
                baseProfile.projectileType(),
                rangeTiles
        );
    }

    private static ShooterPlantFoodPhase createGiantPeaPhase(
            ShooterProfile baseProfile,
            int startDelayTicks,
            int totalShots,
            HorizontalDirection direction
    ) {
        return new ShooterPlantFoodPhase(
                startDelayTicks,
                Math.multiplyExact(
                        totalShots,
                        GIANT_PROJECTILE_DURATION_TICKS
                ),
                GIANT_PROJECTILE_DURATION_TICKS,
                baseProfile.damagePerProjectile()
                        * GIANT_PEA_DAMAGE_MULTIPLIER,
                singleLanePaths(totalShots, direction),
                baseProfile.projectileType(),
                baseProfile.rangeTiles()
        );
    }

    private static List<PlantFoodShotPath> singleLanePaths(
            int totalShots,
            HorizontalDirection direction
    ) {
        return List.of(
                new PlantFoodShotPath(
                        0,
                        direction,
                        totalShots
                )
        );
    }
}
