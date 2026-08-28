package pvz.model.entity.plant.category.melee;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.level.PlantUpgradeType;

final class MeleeProfiles {

    private static final Map<String, MeleeKind> KINDS_BY_NAME = Map.of(
            "bonk choy", MeleeKind.BONK_CHOY,
            "phat beet", MeleeKind.PHAT_BEET,
            "chomper", MeleeKind.CHOMPER,
            "wasabi whip", MeleeKind.WASABI_WHIP,
            "kiwibeast", MeleeKind.KIWIBEAST
    );

    private MeleeProfiles() {
    }

    static MeleeProfile from(PlantSpec spec) {
        MeleeKind kind = kindOf(spec);
        if (kind == null) {
            return null;
        }
        Map<String, Double> params = new HashMap<>(spec.behaviorParams(kind.name()));
        double rangeBonus = spec.getUpgradeValue(PlantUpgradeType.RANGE_TILES_ADD);
        if (rangeBonus != 0) {
            params.merge("rangeTiles", rangeBonus, Double::sum);
        }
        if (kind == MeleeKind.CHOMPER) {
            double digestSeconds = spec.getUpgradeValue(PlantUpgradeType.CHOMPER_DIGEST_SECONDS_ADD);
            if (digestSeconds != 0) {
                params.merge("digestTicks", digestSeconds * pvz.model.core.Game.TICKS_PER_SECOND, Double::sum);
            }
            params.computeIfPresent("digestTicks", (ignored, value) -> Math.max(1, value));
        }
        if (kind == MeleeKind.KIWIBEAST) {
            applyKiwibeastLevelData(spec, params);
        }
        return new MeleeProfile(
                kind,
                damageOf(kind, spec),
                spec.getActionInterval(),
                params
        );
    }

    static boolean isSupported(PlantSpec spec) {
        return kindOf(spec) != null;
    }

    static boolean supportsPlantFood(PlantSpec spec) {
        MeleeProfile profile = from(spec);
        return profile != null
                && profile.supportsPlantFood()
                && spec.hasPlantFoodEffect();
    }


    private static void applyKiwibeastLevelData(PlantSpec spec, Map<String, Double> params) {
        String[] stages = spec.getDamage().strip().split("/");
        if (stages.length != 3) {
            throw new IllegalArgumentException("Kiwibeast damage must contain three stages");
        }
        double stageOne = parseDamage(stages[0], spec.getName());
        double stageTwo = parseDamage(stages[1], spec.getName());
        double stageThree = parseDamage(stages[2], spec.getName());
        params.put("stageTwoDamage", stageTwo);
        params.put("stageThreeDamage", stageThree);

        int maxStage = 3 + (int) Math.round(spec.getUpgradeValue(PlantUpgradeType.KIWI_MAX_STAGE_ADD));
        params.put("maxGrowthStage", (double) maxStage);
        if (maxStage >= 4) {
            double stageFourDamage = stageThree + Math.max(0, stageThree - stageTwo);
            double stageTwoTicks = params.getOrDefault("stageTwoTicks", 240d);
            double stageThreeTicks = params.getOrDefault("stageThreeTicks", 720d);
            double stageFourTicks = stageThreeTicks + Math.max(1, stageThreeTicks - stageTwoTicks);
            params.put("stageFourDamage", stageFourDamage);
            params.put("stageFourTicks", stageFourTicks);
            params.put("plantFoodGrowthStage", 4d);
        }
    }

    private static MeleeKind kindOf(PlantSpec spec) {
        if (spec == null || spec.getCategory() != PlantCategory.MELEE) {
            return null;
        }
        return KINDS_BY_NAME.get(
                spec.getName().strip().toLowerCase(Locale.ROOT)
        );
    }

    private static double damageOf(MeleeKind kind, PlantSpec spec) {
        if (kind == MeleeKind.CHOMPER) {
            return 0;
        }
        if (kind == MeleeKind.KIWIBEAST) {
            return parseFirstDamage(spec);
        }
        return parseDamage(spec.getDamage(), spec.getName());
    }

    private static double parseFirstDamage(PlantSpec spec) {
        String[] stages = spec.getDamage().strip().split("/");
        if (stages.length != 3) {
            throw new IllegalArgumentException(
                    "Kiwibeast damage must contain three stages"
            );
        }
        return parseDamage(stages[0], spec.getName());
    }

    private static double parseDamage(String value, String name) {
        try {
            double damage = Double.parseDouble(value.strip());
            if (damage <= 0) {
                throw new IllegalArgumentException(
                        "melee damage must be positive for " + name
                );
            }
            return damage;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "invalid melee damage for " + name + ": " + value,
                    exception
            );
        }
    }
}
