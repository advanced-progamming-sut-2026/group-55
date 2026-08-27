package pvz.model.entity.plant.category.modifier;

import java.util.Locale;
import java.util.Map;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;

final class ModifierProfiles {

    private static final int MAX_LILY_PAD_CLONES = 4;

    private static final Map<String, ModifierKind> KINDS_BY_NAME = Map.of(
            "hypno-shroom", ModifierKind.HYPNO_SHROOM,
            "lily pad", ModifierKind.LILY_PAD,
            "torchwood", ModifierKind.TORCHWOOD,
            "imitater", ModifierKind.IMITATER
    );

    private ModifierProfiles() {
    }

    static ModifierKind kindOf(PlantSpec spec) {
        if (spec == null || spec.getCategory() != PlantCategory.MODIFIER) {
            return null;
        }

        return KINDS_BY_NAME.get(
                spec.getName().strip().toLowerCase(Locale.ROOT)
        );
    }

    static boolean isSupported(PlantSpec spec) {
        return kindOf(spec) != null;
    }

    static boolean supportsPlantFood(PlantSpec spec) {
        ModifierKind kind = kindOf(spec);

        return (kind == ModifierKind.HYPNO_SHROOM
                || kind == ModifierKind.LILY_PAD
                || kind == ModifierKind.TORCHWOOD
                || kind == ModifierKind.IMITATER)
                && spec.hasPlantFoodEffect();
    }

    static double torchwoodPeaDamageMultiplier(PlantSpec spec) {
        double multiplier = requiredParam(
                spec,
                ModifierKind.TORCHWOOD,
                "peaDamageMultiplier"
        );

        if (!Double.isFinite(multiplier) || multiplier <= 1) {
            throw new IllegalStateException(
                    "Torchwood peaDamageMultiplier must be greater than 1"
            );
        }

        return multiplier;
    }

    static double torchwoodPlantFoodPeaDamageMultiplier(PlantSpec spec) {
        double normalMultiplier = torchwoodPeaDamageMultiplier(spec);
        double multiplier = requiredParam(
                spec,
                ModifierKind.TORCHWOOD,
                "plantFoodPeaDamageMultiplier"
        );

        if (!Double.isFinite(multiplier)
                || multiplier <= normalMultiplier) {
            throw new IllegalStateException(
                    "Torchwood plant food multiplier must exceed its normal multiplier"
            );
        }

        return multiplier;
    }

    static long torchwoodBlueFlameDurationTicks(PlantSpec spec) {
        double value = requiredParam(
                spec,
                ModifierKind.TORCHWOOD,
                "plantFoodBlueFlameDurationTicks"
        );

        if (!Double.isFinite(value)
                || value != Math.rint(value)
                || value <= 0
                || value > Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "Torchwood blue flame duration must be a positive integer"
            );
        }

        return (long) value;
    }

    static int lilyPadCloneCount(PlantSpec spec) {
        double value = requiredParam(
                spec,
                ModifierKind.LILY_PAD,
                "plantFoodCloneCount"
        );

        if (value != Math.rint(value)
                || value <= 0
                || value > MAX_LILY_PAD_CLONES) {
            throw new IllegalStateException(
                    "Lily Pad plantFoodCloneCount must be an integer from 1 to "
                            + MAX_LILY_PAD_CLONES
            );
        }

        return (int) value;
    }

    private static double requiredParam(
            PlantSpec spec,
            ModifierKind kind,
            String param
    ) {
        Double value = spec.behaviorParams(kind.name()).get(param);

        if (value == null) {
            throw new IllegalStateException(
                    "missing modifier parameter " + param
                            + " for behavior " + kind
            );
        }

        return value;
    }
}
