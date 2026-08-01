package pvz.model.entity.plant.plantfood;

public final class PlantFoodState {

    private long activeUntilTick = 0L;

    public boolean tryActivate(long currentTick, long durationTicks) {
        validateTick(currentTick);

        if (durationTicks <= 0) {
            throw new IllegalArgumentException("plant food duration must be positive");
        }

        if (isActive(currentTick)) {
            return false;
        }

        activeUntilTick = currentTick + durationTicks;
        return true;
    }

    public boolean isActive(long currentTick) {
        validateTick(currentTick);
        return currentTick < activeUntilTick;
    }

    public long getRemainingTicks(long currentTick) {
        validateTick(currentTick);

        return Math.max(0, activeUntilTick - currentTick);
    }

    private void validateTick(long tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("current tick cannot be negative");
        }
    }
}
