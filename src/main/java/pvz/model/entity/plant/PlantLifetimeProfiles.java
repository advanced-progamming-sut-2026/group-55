package pvz.model.entity.plant;

import java.util.Locale;
import java.util.Objects;
import pvz.model.core.Game;

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
                    new PlantLifetimeProfile(SHORT_LIVED_SHROOM_LIFETIME_TICKS);

            default -> null;
        };
    }
}