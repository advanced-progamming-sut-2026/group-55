package pvz.model.entity.plant.lifetime;

import java.util.Locale;
import java.util.Objects;
import pvz.model.core.Game;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.level.PlantUpgradeType;

public final class PlantLifetimeProfiles {
    private static final long SHORT_LIVED_SHROOM_LIFETIME_TICKS = 60L * Game.TICKS_PER_SECOND;

    private PlantLifetimeProfiles() {
    }

    public static PlantLifetimeProfile from(
            PlantSpec spec
    ) {
        Objects.requireNonNull(spec, "plant spec cannot be null");

        return switch (spec.getName().toLowerCase(Locale.ROOT)) {
            case "puff-shroom", "sea-shroom" ->
                    new PlantLifetimeProfile(
                            SHORT_LIVED_SHROOM_LIFETIME_TICKS
                                    + Math.round(spec.getUpgradeValue(
                                    PlantUpgradeType.LIFESPAN_SECONDS_ADD)
                                    * Game.TICKS_PER_SECOND)
                    );

            default -> null;
        };
    }
}
