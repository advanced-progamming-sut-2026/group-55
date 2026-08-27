package pvz.model.entity.plant.category.homing;

import java.util.Locale;
import java.util.Map;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;

final class HomingProfiles {

    private static final Map<String, HomingKind> KINDS_BY_NAME = Map.of(
            "caulipower", HomingKind.CAULIPOWER,
            "electric blueberry", HomingKind.ELECTRIC_BLUEBERRY,
            "magnet-shroom", HomingKind.MAGNET_SHROOM,
            "cat-tail", HomingKind.CAT_TAIL
    );

    private HomingProfiles() {
    }

    static HomingProfile from(PlantSpec spec) {
        HomingKind kind = kindOf(spec);

        if (kind == null) {
            return null;
        }

        return new HomingProfile(
                kind,
                damageOf(kind, spec),
                spec.getActionInterval(),
                spec.behaviorParams(kind.name())
        );
    }

    static boolean isSupported(PlantSpec spec) {
        return kindOf(spec) != null;
    }

    static boolean supportsPlantFood(PlantSpec spec) {
        HomingProfile profile = from(spec);

        return profile != null
                && profile.supportsPlantFood()
                && spec.hasPlantFoodEffect();
    }

    private static HomingKind kindOf(PlantSpec spec) {
        if (spec == null || spec.getCategory() != PlantCategory.HOMING) {
            return null;
        }

        return KINDS_BY_NAME.get(
                spec.getName().strip().toLowerCase(Locale.ROOT)
        );
    }

    private static double damageOf(HomingKind kind, PlantSpec spec) {
        if (kind == HomingKind.CAULIPOWER
                || kind == HomingKind.MAGNET_SHROOM) {
            return 0;
        }

        return parseDamage(spec);
    }

    private static double parseDamage(PlantSpec spec) {
        try {
            double damage = Double.parseDouble(spec.getDamage().strip());

            if (damage <= 0) {
                throw new IllegalArgumentException(
                        "homing damage must be positive for "
                                + spec.getName()
                );
            }

            return damage;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "invalid homing damage for "
                            + spec.getName()
                            + ": "
                            + spec.getDamage(),
                    exception
            );
        }
    }
}
