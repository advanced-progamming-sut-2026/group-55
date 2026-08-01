package pvz.model.entity.plant.lifetime;

import java.util.Objects;

import pvz.model.entity.plant.PlantSpec;

public final class PlantLifetimeController {

    private final PlantLifetimeProfile profile;

    private long expirationTick = Long.MAX_VALUE;

    private boolean started;

    private PlantLifetimeController(PlantLifetimeProfile profile) {
        this.profile = profile;
    }

    public static PlantLifetimeController from(PlantSpec spec) {
        Objects.requireNonNull(spec, "plant spec cannot be null");

        return new PlantLifetimeController(PlantLifetimeProfiles.from(spec));
    }

    public void resetAt(long startTick) {
        validateTick(startTick);

        started = true;

        if (profile == null) {
            expirationTick = Long.MAX_VALUE;
            return;
        }

        expirationTick = startTick + profile.lifespanTicks();
    }

    public boolean isExpired(long currentTick) {
        validateTick(currentTick);

        return started // 1
                && profile != null // 2
                && currentTick >= expirationTick; // 3
    }

    private static void validateTick(long tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("lifetime tick cannot be negative");
        }
    }
}
