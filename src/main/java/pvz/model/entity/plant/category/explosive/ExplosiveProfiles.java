package pvz.model.entity.plant.category.explosive;

import java.util.Locale;
import java.util.Map;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;

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

        return new ExplosiveProfile(
                kind,
                damageOf(kind, spec),
                spec.behaviorParams(kind.name())
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
                    GLOBAL_BLAST -> parseDamage(spec);

            default -> 0;
        };
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
