package pvz.model.entity.plant.category.melee;

import java.util.Locale;
import java.util.Map;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;

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
        return new MeleeProfile(
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
        MeleeProfile profile = from(spec);
        return profile != null
                && profile.supportsPlantFood()
                && spec.hasPlantFoodEffect();
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
