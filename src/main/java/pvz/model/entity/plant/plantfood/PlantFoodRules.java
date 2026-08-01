package pvz.model.entity.plant.plantfood;

import pvz.model.core.Game;

public final class PlantFoodRules {

    public static final long MINIMUM_DURATION_TICKS = 2L * Game.TICKS_PER_SECOND;

    private PlantFoodRules() {
    }

    public static long effectiveDurationTicks(long requestedDurationTicks) {
        if (requestedDurationTicks <= 0) {
            throw new IllegalStateException(
                    "requested plant food duration must be positive"
            );
        }

        return Math.max(MINIMUM_DURATION_TICKS, requestedDurationTicks
        );
    }
}
