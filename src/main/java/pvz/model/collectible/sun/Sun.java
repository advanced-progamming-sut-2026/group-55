package pvz.model.collectible.sun;

import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.entity.plant.Plant;

public final class Sun {
    private static final long AVAILABLE_LIFETIME_TICKS =
            5L * Game.TICKS_PER_SECOND;

    private static final double FALL_SPEED_PER_TICK = 0.1;

    private final SunSource source;
    private final Plant producer;
    private final int value;

    private final double targetX;
    private final double targetY;

    private double currentX;
    private double currentY;

    private SunType type;
    private SunState state;
    private long availableTicksLeft;

    private Sun(
            SunSource source,
            Plant producer,
            SunType type,
            int value,
            double currentX,
            double currentY,
            double targetX,
            double targetY,
            SunState state
    ) {
        this.source = Objects.requireNonNull(source);
        this.producer = producer;
        this.type = Objects.requireNonNull(type);
        this.value = value;
        this.currentX = currentX;
        this.currentY = currentY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.state = Objects.requireNonNull(state);

        if (state == SunState.AVAILABLE) {
            availableTicksLeft = AVAILABLE_LIFETIME_TICKS;
        }
    }

    public static Sun fromPlant(
            Plant producer,
            double x,
            double y,
            int value
    ) {
        return new Sun(
                SunSource.PLANT,
                Objects.requireNonNull(producer),
                SunType.NORMAL,
                value,
                x,
                y,
                x,
                y,
                SunState.AVAILABLE
        );
    }

    public static Sun fromSky(
            SunType type,
            double targetX,
            double startY,
            double targetY,
            int value
    ) {
        return new Sun(
                SunSource.SKY,
                null,
                type,
                value,
                targetX,
                startY,
                targetX,
                targetY,
                SunState.FALLING
        );
    }

    public void update() {
        if (state == SunState.FALLING) {
            updateFalling();
        }
        else if (state == SunState.AVAILABLE) {
            updateAvailable();
        }
    }

    private void updateFalling() {
        double remainingDistance = currentY - targetY;

        if (remainingDistance <= FALL_SPEED_PER_TICK) {
            currentX = targetX;
            currentY = targetY;
            state = SunState.AVAILABLE;
            availableTicksLeft = AVAILABLE_LIFETIME_TICKS;

            if (type == SunType.RADIOACTIVE) {
                type = SunType.NORMAL;
            }
            return;
        }
        currentY -= FALL_SPEED_PER_TICK;
    }

    private void updateAvailable() {
        availableTicksLeft--;

        if (availableTicksLeft <= 0) {
            state = SunState.REMOVED;
        }
    }

    void remove() {
        state = SunState.REMOVED;
    }

    public int getTileX() {
        return (int) Math.floor(currentX) + 1;
    }

    public int getTileY() {
        return (int) Math.floor(currentY) + 1;
    }

    public boolean isFalling() {
        return state == SunState.FALLING;
    }

    public boolean isRemoved() {
        return state == SunState.REMOVED;
    }

    public SunType getType() {
        return type;
    }

    public SunSource getSource() {
        return source;
    }

    public SunState getState() {
        return state;
    }

    public Plant getProducer() {
        return producer;
    }

    public int getValue() {
        return value;
    }

    public double getCurrentX() {
        return currentX;
    }

    public double getCurrentY() {
        return currentY;
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }
}