package pvz.model.entity.plant.shooter;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.projectile.ProjectileType;

public final class ShooterProfiles {
    private static final double DEFAULT_SHOT_DAMAGE = 20;
    private static final long RAPID_SHOT_GAP_TICKS = 3;
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
            case "peashooter" , "pea pod" -> singleLaneProfile(
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

            case "snow pea" -> singleLaneProfile(
                    parseSimpleDamage(spec.getDamage()),
                    1,
                    0,
                    ProjectileType.ICE
            );

            case "rotobaga" -> createRotobagaProfile(spec);

            case "threepeater" -> new ShooterProfile(
                    20,
                    0,
                    List.of(
                            new ShotPath(-1, ShotVector.RIGHT, 1),
                            new ShotPath(0, ShotVector.RIGHT, 1),
                            new ShotPath(1, ShotVector.RIGHT, 1)
                    ),
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

            case "split pea" -> new ShooterProfile(
                    20,
                    RAPID_SHOT_GAP_TICKS,
                    List.of(
                            new ShotPath(0, ShotVector.RIGHT, 1),
                            new ShotPath(0, ShotVector.LEFT, 2)
                    ),
                    ProjectileType.NORMAL,
                    FULL_BOARD_RANGE
            );
            case "citron" -> singleLaneProfile(
                    800,
                    1,
                    0,
                    ProjectileType.NORMAL
            );

            case "starfruit" -> createStarfruitProfile(spec);

            case "goo peashooter" -> singleLaneProfile(
                    parseSimpleDamage(spec.getDamage()),
                    1,
                    0,
                    ProjectileType.POISON
            );

            default -> createFallbackProfile(spec);
        };
    }

    private static ShooterProfile createRotobagaProfile(
            PlantSpec spec
    ) {
        DamageBurst damageBurst = parseDamageBurst(
                spec.getDamage()
        );

        return new ShooterProfile(
                damageBurst.damagePerProjectile(),
                RAPID_SHOT_GAP_TICKS,
                List.of(
                        new ShotPath(
                                0,
                                ShotVector.UP_RIGHT,
                                damageBurst.shotsPerDirection()
                        ),
                        new ShotPath(
                                0,
                                ShotVector.DOWN_RIGHT,
                                damageBurst.shotsPerDirection()
                        ),
                        new ShotPath(
                                0,
                                ShotVector.UP_LEFT,
                                damageBurst.shotsPerDirection()
                        ),
                        new ShotPath(
                                0,
                                ShotVector.DOWN_LEFT,
                                damageBurst.shotsPerDirection()
                        )
                ),
                ProjectileType.NORMAL,
                FULL_BOARD_RANGE
        );
    }

    private static ShooterProfile createStarfruitProfile(
            PlantSpec spec
    ) {
        double damage = parseSimpleDamage(
                spec.getDamage()
        );

        return new ShooterProfile(
                damage,
                0,
                List.of(
                        new ShotPath(0, ShotVector.LEFT, 1),
                        new ShotPath(0, ShotVector.UP, 1),
                        new ShotPath(0, ShotVector.DOWN, 1),
                        new ShotPath(
                                0,
                                ShotVector.SHALLOW_UP_RIGHT,
                                1
                        ),
                        new ShotPath(
                                0,
                                ShotVector.SHALLOW_DOWN_RIGHT,
                                1
                        )
                ),
                ProjectileType.NORMAL,
                FULL_BOARD_RANGE
        );
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
                ticksBetweenShots,
                List.of(
                        new ShotPath(
                                0,
                                ShotVector.RIGHT,
                                shotsPerLane
                        )
                ),
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

    private static DamageBurst parseDamageBurst(
            String damageText
    ) {
        String[] parts = damageText
                .strip()
                .toLowerCase(Locale.ROOT)
                .split("x");

        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "expected repeated damage in the form damagexshots: "
                            + damageText
            );
        }

        try {
            double damage = Double.parseDouble(parts[0]);
            int shots = Integer.parseInt(parts[1]);

            if (damage < 0 || shots <= 0) {
                throw new IllegalArgumentException(
                        "repeated damage values must be positive: "
                                + damageText
                );
            }

            return new DamageBurst(damage, shots);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "invalid repeated damage value: "
                            + damageText,
                    exception
            );
        }
    }

    private record DamageBurst(
            double damagePerProjectile,
            int shotsPerDirection
    ) {
    }
}
