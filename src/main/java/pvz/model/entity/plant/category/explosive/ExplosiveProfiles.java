package pvz.model.entity.plant.category.explosive;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.level.PlantUpgradeType;
import pvz.model.core.Game;

public final class ExplosiveProfiles {

    private static final Map<String, ExplosiveKind> KINDS_BY_NAME = Map.ofEntries(
            Map.entry("potato mine", ExplosiveKind.MINE),
            Map.entry("primal potato mine", ExplosiveKind.MINE),
            Map.entry("cherry bomb", ExplosiveKind.INSTANT_BLAST),
            Map.entry("squash", ExplosiveKind.SQUASH),
            Map.entry("grapeshot", ExplosiveKind.GRAPESHOT),
            Map.entry("jalapeno", ExplosiveKind.ROW_BLAST),
            Map.entry("doom-shroom", ExplosiveKind.GLOBAL_BLAST),
            Map.entry("tangle kelp", ExplosiveKind.TANGLE_KELP),
            Map.entry("iceberg lettuce", ExplosiveKind.FREEZE_TRAP),
            Map.entry("ice-shroom", ExplosiveKind.GLOBAL_FREEZE),
            Map.entry("hot potato", ExplosiveKind.TILE_MELT),
            Map.entry("grave buster", ExplosiveKind.TOMBSTONE_DESTROY)
    );

    private ExplosiveProfiles() {
    }

    public static ExplosiveProfile from(PlantSpec spec) {
        ExplosiveKind kind = kindOf(spec);

        if (kind == null) {
            return null;
        }

        Map<String, Double> params = new HashMap<>(spec.behaviorParams(kind.name()));
        applyLevelUpgrades(kind, spec, params);
        return new ExplosiveProfile(
                kind,
                damageOf(kind, spec),
                params
        );
    }

    public static boolean isSupported(PlantSpec spec) {
        return kindOf(spec) != null;
    }

    public static boolean supportsPlantFood(PlantSpec spec) {
        ExplosiveProfile profile = from(spec);

        return profile != null
                && profile.supportsPlantFood()
                && spec.hasPlantFoodEffect();
    }


    private static void applyLevelUpgrades(
            ExplosiveKind kind, PlantSpec spec, Map<String, Double> params
    ) {
        double armSeconds = spec.getUpgradeValue(PlantUpgradeType.MINE_ARM_SECONDS_ADD);
        if (armSeconds != 0 && kind == ExplosiveKind.MINE) {
            params.merge("armDelayTicks", armSeconds * Game.TICKS_PER_SECOND, Double::sum);
            params.computeIfPresent("armDelayTicks", (ignored, value) -> Math.max(1, value));
        }

        if (kind == ExplosiveKind.SQUASH
                && spec.hasUpgrade(PlantUpgradeType.SQUASH_MAX_ACTIVATIONS_SET)) {
            params.put("maxActivations", spec.getLatestUpgradeValue(
                    PlantUpgradeType.SQUASH_MAX_ACTIVATIONS_SET, 1));
        }
        if (kind == ExplosiveKind.GRAPESHOT) {
            params.merge("maxBounces",
                    spec.getUpgradeValue(PlantUpgradeType.GRAPESHOT_BOUNCES_ADD), Double::sum);
        }
        if (kind == ExplosiveKind.TANGLE_KELP) {
            params.put("normalTargetCount", 1d + spec.getUpgradeValue(
                    PlantUpgradeType.TANGLE_NORMAL_TARGETS_ADD));
        }
        if (kind == ExplosiveKind.FREEZE_TRAP || kind == ExplosiveKind.GLOBAL_FREEZE) {
            params.merge("freezeDurationTicks",
                    spec.getUpgradeValue(PlantUpgradeType.FREEZE_SECONDS_ADD)
                            * Game.TICKS_PER_SECOND, Double::sum);
        }
        if (kind == ExplosiveKind.TILE_MELT
                && spec.hasUpgrade(PlantUpgradeType.HOT_POTATO_MELT_RADIUS_SET)) {
            params.put("meltRadius", spec.getLatestUpgradeValue(
                    PlantUpgradeType.HOT_POTATO_MELT_RADIUS_SET, 0));
        }
        double finishDamage = spec.getUpgradeValue(PlantUpgradeType.FINISH_EXPLOSION_DAMAGE);
        if (finishDamage > 0) {
            params.put("finishExplosionDamage", finishDamage);
        }
        if (kind == ExplosiveKind.TOMBSTONE_DESTROY) {
            double seconds = spec.getUpgradeValue(PlantUpgradeType.GRAVE_BUSTER_EAT_SECONDS_ADD);
            if (seconds != 0) {
                params.merge("effectDisplayTicks", seconds * Game.TICKS_PER_SECOND, Double::sum);
                params.computeIfPresent("effectDisplayTicks", (ignored, value) -> Math.max(1, value));
            }
        }
    }

    private static ExplosiveKind kindOf(PlantSpec spec) {
        if (spec == null || spec.getCategory() != PlantCategory.EXPLOSIVE) {
            return null;
        }

        return KINDS_BY_NAME.get(
                spec.getName().strip().toLowerCase(Locale.ROOT)
        );
    }

    private static double damageOf(ExplosiveKind kind, PlantSpec spec) {
        return switch (kind) {
            case MINE,
                    INSTANT_BLAST,
                    SQUASH,
                    GRAPESHOT,
                    ROW_BLAST,
                    GLOBAL_BLAST,
                    GLOBAL_FREEZE -> parseDamageAllowZero(spec);

            default -> 0;
        };
    }

    private static double parseDamageAllowZero(PlantSpec spec) {
        try {
            double damage = Double.parseDouble(spec.getDamage().strip());
            if (damage < 0) {
                throw new IllegalArgumentException("explosive damage cannot be negative for " + spec.getName());
            }
            return damage;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "invalid damage value for " + spec.getName() + ": " + spec.getDamage(), exception);
        }
    }

    private static double parseDamage(PlantSpec spec) {
        try {
            double damage = Double.parseDouble(spec.getDamage().strip());

            if (damage <= 0) {
                throw new IllegalArgumentException(
                        "explosive damage must be positive for "
                                + spec.getName()
                );
            }

            return damage;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "invalid damage value for "
                            + spec.getName()
                            + ": "
                            + spec.getDamage(),
                    exception
            );
        }
    }
}
