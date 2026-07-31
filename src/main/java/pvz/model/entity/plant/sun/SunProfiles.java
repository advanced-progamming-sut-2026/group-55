package pvz.model.entity.plant.sun;

import java.util.Locale;
import java.util.Objects;

import pvz.model.entity.collectible.sun.SunValue;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.sun.FixedSunProfile;
import pvz.model.entity.plant.sun.SunProfile;
import pvz.model.entity.plant.sun.SunShroomProfile;

public final class SunProfiles {

    private SunProfiles() {
    }

    public static SunProfile from(
            PlantSpec spec,
            long plantedTick
    ) {
        Objects.requireNonNull(
                spec,
                "plant spec cannot be null"
        );

        if (spec.getCategory() != PlantCategory.SUN_PRODUCER) {
            throw new IllegalArgumentException(
                    spec.getName() + " is not a sun producer"
            );
        }

        return switch (spec.getName().strip().toLowerCase(Locale.ROOT)) {
            case "sunflower" ->
                    new FixedSunProfile(
                            SunValue.NORMALSUN.getValue(),
                            1,
                            SunValue.FNSUN.getValue()
                    );

            case "twin sunflower" ->
                    new FixedSunProfile(
                            SunValue.NORMALSUN.getValue(),
                            2,
                            SunValue.FTSUN.getValue()
                    );

            case "primal sunflower" ->
                    new FixedSunProfile(
                            SunValue.BIGSUN.getValue(),
                            1,
                            SunValue.FBSUN.getValue()
                    );

            case "sun-shroom" ->
                    new SunShroomProfile(
                            plantedTick
                    );

            default ->
                    throw new IllegalArgumentException(
                            "missing sun profile for "
                                    + spec.getName()
                    );
        };
    }
}
