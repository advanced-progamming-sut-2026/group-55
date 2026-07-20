package pvz.model.entity.collectible.sun;

import java.util.Objects;

import pvz.model.entity.collectible.Collectible;
import pvz.model.core.Game;
import pvz.model.entity.plant.Plant;
import pvz.model.core.World;

public final class Sun extends Collectible {
    private static final long AVAILABLE_LIFETIME_TICKS =
            8L * Game.TICKS_PER_SECOND;

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

    private final World world;

    private Sun(
            World world,
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
        this.world = Objects.requireNonNull(world);
        this.name = "sun";
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
            World world,
            Plant producer,
            double x,
            double y,
            int value
    ) {
        return new Sun(
                world,
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
            World world,
            SunType type,
            double targetX,
            double startY,
            double targetY,
            int value
    ) {
        return new Sun(
                world,
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

    @Override
    public void update(long tick) {
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
            remove();
        }
    }

    public void remove() {
        if (state == SunState.REMOVED) {
            return;
        }

        state = SunState.REMOVED;

        if (producer != null) {
            producer.onProducedSunRemoved();
        }

        world.removeCollectible(this);
        world.game().unregister(this);
    }

    @Override
    public double getX() {
        return currentX;
    }

    @Override
    public double getY() {
        return currentY;
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

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }
}