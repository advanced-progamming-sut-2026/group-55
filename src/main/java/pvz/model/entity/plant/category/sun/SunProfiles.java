package pvz.model.entity.plant.category.sun;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongFunction;

import pvz.model.entity.collectible.sun.SunValue;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;

public final class SunProfiles {

    private static final Map<String, SunProfileDefinition>
            PROFILE_DEFINITIONS = Map.of(
                    "sunflower",
                    new SunProfileDefinition(
                            ignoredTick ->
                                    new FixedSunProfile(
                                            SunValue.NORMALSUN.getValue(),
                                            1,
                                            SunValue.FNSUN.getValue()
                                    )
                    ),

                    "twin sunflower",
                    new SunProfileDefinition(
                            ignoredTick ->
                                    new FixedSunProfile(
                                            SunValue.NORMALSUN.getValue(),
                                            2,
                                            SunValue.FTSUN.getValue()
                                    )
                    ),

                    "primal sunflower",
                    new SunProfileDefinition(
                            ignoredTick ->
                                    new FixedSunProfile(
                                            SunValue.BIGSUN.getValue(),
                                            1,
                                            SunValue.FBSUN.getValue()
                                    )
                    ),

                    "sun-shroom",
                    new SunProfileDefinition(SunShroomProfile::new),

                    "gold bloom",
                    new SunProfileDefinition(
                            ignoredTick -> new GoldBloomProfile()
                    )
            );

    private SunProfiles() {
    }

    public static SunProfile from(PlantSpec spec, long plantedTick) {
        Objects.requireNonNull(
                spec,
                "plant spec cannot be null"
        );

        if (spec.getCategory() != PlantCategory.SUN_PRODUCER) {
            throw new IllegalArgumentException(
                    spec.getName() + " is not a sun producer"
            );
        }

        SunProfileDefinition definition = definitionFor(spec);

        if (definition == null) {
            throw new IllegalArgumentException(
                    "missing sun profile for " + spec.getName()
            );
        }

        return definition.factory().apply(plantedTick);
    }

    public static boolean hasProfileFor(PlantSpec spec) {
        return definitionFor(spec) != null;
    }

    public static boolean supportsPlantFood(PlantSpec spec) {
        SunProfileDefinition definition = definitionFor(spec);

        if (definition == null || !spec.hasPlantFoodEffect()) {
            return false;
        }

        return definition.factory().apply(0).supportsPlantFood();
    }

    private static SunProfileDefinition definitionFor(PlantSpec spec) {
        if (spec == null
                || spec.getCategory() != PlantCategory.SUN_PRODUCER) {
            return null;
        }

        return PROFILE_DEFINITIONS.get(normalize(spec.getName()));
    }

    private static String normalize(String plantName) {
        return plantName
                .strip()
                .toLowerCase(Locale.ROOT);
    }

    private record SunProfileDefinition(
            LongFunction<SunProfile> factory
    ) {
        private SunProfileDefinition {
            Objects.requireNonNull(
                    factory,
                    "sun profile factory cannot be null"
            );
        }
    }
}
