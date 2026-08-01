package pvz.model.entity.plant.sun;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongFunction;

import pvz.model.entity.collectible.sun.SunValue;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;

public final class SunProfiles {

    private static final Map<String, LongFunction<SunProfile>>
            PROFILE_FACTORIES = Map.of(
                    "sunflower",
                    ignoredTick ->
                            new FixedSunProfile(
                                    SunValue.NORMALSUN.getValue(),
                                    1,
                                    SunValue.FNSUN.getValue()
                            ),

                    "twin sunflower",
                    ignoredTick ->
                            new FixedSunProfile(
                                    SunValue.NORMALSUN.getValue(),
                                    2,
                                    SunValue.FTSUN.getValue()
                            ),

                    "primal sunflower",
                    ignoredTick ->
                            new FixedSunProfile(
                                    SunValue.BIGSUN.getValue(),
                                    1,
                                    SunValue.FBSUN.getValue()
                            ),

                    "sun-shroom",
                    SunShroomProfile::new
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

        LongFunction<SunProfile> profileFactory = PROFILE_FACTORIES.get(normalize(spec.getName()));

        if (profileFactory == null) {
            throw new IllegalArgumentException(
                    "missing sun profile for " + spec.getName()
            );
        }

        return profileFactory.apply(
                plantedTick
        );
    }

    public static boolean hasProfileFor(PlantSpec spec) {
        return spec != null
                && spec.getCategory() == PlantCategory.SUN_PRODUCER
                && PROFILE_FACTORIES.containsKey(normalize(spec.getName())
        );
    }

    private static String normalize(String plantName) {
        return plantName
                .strip()
                .toLowerCase(Locale.ROOT);
    }
}
