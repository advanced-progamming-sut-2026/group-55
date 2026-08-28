package pvz.model.entity.plant.category.lobber;

import java.util.Locale;
import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.plant.level.PlantUpgradeType;

final class LobberProfiles {
    private static final int NO_SPLASH = 0;
    private static final int AREA_SPLASH_RADIUS = 1;
    private static final double BUTTER_CHANCE = 0.30;
    private static final long BUTTER_STUN_TICKS =
            3L * Game.TICKS_PER_SECOND;

    private LobberProfiles() {
    }

    static boolean supports(PlantSpec spec) {
        Objects.requireNonNull(
                spec,
                "plant spec cannot be null"
        );

        if (spec.getCategory() != PlantCategory.LOBBER) {
            return false;
        }

        return switch (normalize(spec.getName())) {
            case "cabbage-pult",
                    "kernel-pult",
                    "melon-pult",
                    "winter melon",
                    "pepper-pult" -> true;
            default -> false;
        };
    }

    static LobberProfile from(PlantSpec spec) {
        Objects.requireNonNull(
                spec,
                "plant spec cannot be null"
        );

        if (spec.getCategory() != PlantCategory.LOBBER) {
            throw new IllegalArgumentException(
                    spec.getName() + " is not a lobber"
            );
        }

        return switch (normalize(spec.getName())) {
            case "cabbage-pult" -> singleShot(
                    spec, parseSimpleDamage(spec), NO_SPLASH, ProjectileType.NORMAL
            );

            case "kernel-pult" -> createKernelProfile(spec);

            case "melon-pult" -> singleShot(
                    spec, parseSimpleDamage(spec), AREA_SPLASH_RADIUS, ProjectileType.NORMAL
            );

            case "winter melon" -> singleShot(
                    spec, parseSimpleDamage(spec), AREA_SPLASH_RADIUS, ProjectileType.ICE
            );

            case "pepper-pult" -> singleShot(
                    spec, parseSimpleDamage(spec), AREA_SPLASH_RADIUS, ProjectileType.FIRE
            );

            default -> throw new IllegalArgumentException(
                    "unsupported lobber plant: " + spec.getName()
            );
        };
    }

    private static LobberProfile createKernelProfile(
            PlantSpec spec
    ) {
        double[] damageValues = parseDamagePair(spec);

        LobberShot kernel = new LobberShot(
                damageValues[0],
                NO_SPLASH,
                ProjectileType.NORMAL,
                0
        );

        LobberShot butter = new LobberShot(
                damageValues[1],
                NO_SPLASH,
                ProjectileType.NORMAL,
                BUTTER_STUN_TICKS
        );

        return new LobberProfile(
                kernel,
                butter,
                Math.min(1, BUTTER_CHANCE + spec.getUpgradeValue(
                        PlantUpgradeType.KERNEL_BUTTER_CHANCE_ADD))
        );
    }

    private static LobberProfile singleShot(
            PlantSpec spec,
            double damage,
            int splashRadius,
            ProjectileType projectileType
    ) {
        double splashBonus = spec.getUpgradeValue(
                PlantUpgradeType.SPLASH_DAMAGE_ADD);
        int warmthRadius = normalize(spec.getName()).equals("pepper-pult")
                ? 1 + (int) Math.round(spec.getUpgradeValue(PlantUpgradeType.WARMTH_RADIUS_ADD))
                : 0;
        return new LobberProfile(
                new LobberShot(
                        damage, splashRadius, projectileType, 0,
                        splashBonus, warmthRadius
                )
        );
    }

    private static double parseSimpleDamage(
            PlantSpec spec
    ) {
        return parsePositiveDamage(
                spec,
                spec.getDamage()
        );
    }

    private static double[] parseDamagePair(
            PlantSpec spec
    ) {
        String[] pieces = spec.getDamage()
                .strip()
                .split("/");

        if (pieces.length != 2) {
            throw new IllegalArgumentException(
                    "kernel-pult damage must use kernel/butter form: "
                            + spec.getDamage()
            );
        }

        return new double[] {
                parsePositiveDamage(spec, pieces[0]),
                parsePositiveDamage(spec, pieces[1])
        };
    }

    private static double parsePositiveDamage(
            PlantSpec spec,
            String text
    ) {
        try {
            double damage = Double.parseDouble(text.strip());

            if (!Double.isFinite(damage) || damage <= 0) {
                throw new IllegalArgumentException(
                        "lobber damage must be finite and positive: "
                                + text
                );
            }

            return damage;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "invalid lobber damage for "
                            + spec.getName()
                            + ": "
                            + text,
                    exception
            );
        }
    }

    private static String normalize(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
