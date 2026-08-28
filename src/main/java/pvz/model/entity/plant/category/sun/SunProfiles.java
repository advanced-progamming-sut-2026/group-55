package pvz.model.entity.plant.category.sun;

import java.util.Locale;
import java.util.Objects;
import pvz.model.entity.collectible.sun.SunValue;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.level.PlantUpgradeType;

public final class SunProfiles {
    private SunProfiles() {}

    public static SunProfile from(PlantSpec spec, long plantedTick) {
        Objects.requireNonNull(spec, "plant spec cannot be null");
        if (spec.getCategory() != PlantCategory.SUN_PRODUCER) {
            throw new IllegalArgumentException(spec.getName() + " is not a sun producer");
        }
        String name = normalize(spec.getName());
        return switch (name) {
            case "sunflower" -> new FixedSunProfile(
                    SunValue.NORMALSUN.getValue(), 1, SunValue.FNSUN.getValue());
            case "twin sunflower" -> new FixedSunProfile(
                    SunValue.NORMALSUN.getValue(), 2, SunValue.FTSUN.getValue());
            case "primal sunflower" -> new FixedSunProfile(
                    SunValue.BIGSUN.getValue(), 1, SunValue.FBSUN.getValue());
            case "sun-shroom" -> new SunShroomProfile(
                    plantedTick,
                    (int) Math.round(spec.getUpgradeValue(
                            PlantUpgradeType.SUN_SHROOM_GROW_SECONDS_ADD))
            );
            case "gold bloom" -> new GoldBloomProfile(
                    (int) Math.round(spec.getUpgradeValue(
                            PlantUpgradeType.SUN_BURST_TOTAL_ADD))
            );
            default -> throw new IllegalArgumentException("missing sun profile for " + spec.getName());
        };
    }

    public static boolean hasProfileFor(PlantSpec spec) {
        if (spec == null || spec.getCategory() != PlantCategory.SUN_PRODUCER) {
            return false;
        }
        return switch (normalize(spec.getName())) {
            case "sunflower", "twin sunflower", "primal sunflower", "sun-shroom", "gold bloom" -> true;
            default -> false;
        };
    }

    public static boolean supportsPlantFood(PlantSpec spec) {
        return hasProfileFor(spec)
                && spec.hasPlantFoodEffect()
                && from(spec, 0).supportsPlantFood();
    }

    private static String normalize(String plantName) {
        return plantName.strip().toLowerCase(Locale.ROOT);
    }
}
