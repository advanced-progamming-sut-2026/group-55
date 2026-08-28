package pvz.model.entity.plant.category.strikethrough;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.attack.ShotPath;
import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.projectile.ProjectileHitLimit;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.plant.level.PlantUpgradeType;

public final class StrikeThroughProfiles {
    private static final int FULL_BOARD_RANGE = Integer.MAX_VALUE;
    private static final int FUME_SHROOM_RANGE_TILES = 4;
    private static final int CACTUS_MAX_HITS = 3;

    private static final Map<String, StrikeThroughKind> KINDS_BY_NAME = Map.of(
            "cactus", StrikeThroughKind.CACTUS,
            "fume-shroom", StrikeThroughKind.FUME_SHROOM
    );

    private StrikeThroughProfiles() {
    }

    public static StrikeThroughProfile from(PlantSpec spec) {
        Objects.requireNonNull(spec, "plant spec cannot be null");

        StrikeThroughKind kind = kindOf(spec);
        if (kind == null) {
            throw new IllegalArgumentException(
                    "unsupported strike-through plant: " + spec.getName()
            );
        }

        return new StrikeThroughProfile(
                kind,
                parseDamage(spec),
                0,
                List.of(new ShotPath(0, ShotVector.RIGHT, 1)),
                ProjectileType.NORMAL,
                rangeTiles(kind, spec),
                hitLimit(kind, spec),
                plantFoodDamageMultiplier(kind, spec),
                plantFoodDamage(kind, spec),
                plantFoodKnockbackTiles(kind, spec)
        );
    }

    public static boolean isSupported(PlantSpec spec) {
        return kindOf(spec) != null;
    }

    public static boolean supportsPlantFood(PlantSpec spec) {
        return isSupported(spec) && spec.hasPlantFoodEffect();
    }

    private static StrikeThroughKind kindOf(PlantSpec spec) {
        if (spec == null || spec.getCategory() != PlantCategory.STRIKE_THROUGH) {
            return null;
        }

        return KINDS_BY_NAME.get(
                spec.getName().strip().toLowerCase(Locale.ROOT)
        );
    }

    private static int rangeTiles(StrikeThroughKind kind, PlantSpec spec) {
        return switch (kind) {
            case CACTUS -> FULL_BOARD_RANGE;
            case FUME_SHROOM -> FUME_SHROOM_RANGE_TILES
                    + (int) Math.round(spec.getUpgradeValue(PlantUpgradeType.RANGE_TILES_ADD));
        };
    }

    private static ProjectileHitLimit hitLimit(StrikeThroughKind kind, PlantSpec spec) {
        return switch (kind) {
            case CACTUS -> ProjectileHitLimit.limitedTo(
                    CACTUS_MAX_HITS
                            + (int) Math.round(spec.getUpgradeValue(
                            PlantUpgradeType.CACTUS_PIERCE_ADD))
            );
            case FUME_SHROOM -> ProjectileHitLimit.unlimited();
        };
    }

    private static double plantFoodDamageMultiplier(
            StrikeThroughKind kind,
            PlantSpec spec
    ) {
        if (kind != StrikeThroughKind.CACTUS) {
            return 1;
        }

        double value = requiredParam(
                spec,
                kind,
                "plantFoodDamageMultiplier"
        );

        if (!Double.isFinite(value) || value <= 1) {
            throw new IllegalStateException(
                    "Cactus plant food damage multiplier must exceed 1"
            );
        }

        return value;
    }

    private static double plantFoodDamage(
            StrikeThroughKind kind,
            PlantSpec spec
    ) {
        if (kind != StrikeThroughKind.FUME_SHROOM) {
            return 0;
        }

        double value = requiredParam(spec, kind, "plantFoodDamage");
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalStateException(
                    "Fume-shroom plant food damage must be positive and finite"
            );
        }
        return value;
    }

    private static double plantFoodKnockbackTiles(
            StrikeThroughKind kind,
            PlantSpec spec
    ) {
        if (kind != StrikeThroughKind.FUME_SHROOM) {
            return 0;
        }

        double value = requiredParam(spec, kind, "plantFoodKnockbackTiles");
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalStateException(
                    "Fume-shroom plant food knockback must be positive and finite"
            );
        }
        return value;
    }

    private static double requiredParam(
            PlantSpec spec,
            StrikeThroughKind kind,
            String param
    ) {
        Double value = spec.behaviorParams(kind.name()).get(param);
        if (value == null) {
            throw new IllegalStateException(
                    "missing strike-through parameter " + param
                            + " for " + spec.getName()
            );
        }
        return value;
    }

    private static double parseDamage(PlantSpec spec) {
        try {
            double damage = Double.parseDouble(spec.getDamage());
            if (!Double.isFinite(damage) || damage < 0) {
                throw new IllegalArgumentException(
                        "strike-through damage must be finite and non-negative: "
                                + spec.getDamage()
                );
            }
            return damage;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "invalid strike-through damage: " + spec.getDamage(),
                    exception
            );
        }
    }
}
