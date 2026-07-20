package pvz.model.entity.collectible.sun;

import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.collectible.Collectible;
import pvz.model.entity.plant.Plant;

public final class Sun extends Collectible {
    private static final long SKY_FALL_DURATION_TICKS =
            5L * Game.TICKS_PER_SECOND;

    private static final long AVAILABLE_LIFETIME_TICKS =
            8L * Game.TICKS_PER_SECOND;

    private final World world;
    private final SunSource source;
    private final Plant producer;
    private final int value;

    private final double targetX;
    private final double targetY;

    private final long landingTick;

    private SunType type;
    private SunState state;
    private long availableTicksLeft;

    private Sun(
            World world,
            SunSource source,
            Plant producer,
            SunType type,
            int value,
            double targetX,
            double targetY,
            long spawnTick,
            long landingTick,
            SunState state
    ) {
        this.world = Objects.requireNonNull(world);
        this.name = "sun";
        this.source = Objects.requireNonNull(source);
        this.producer = producer;
        this.type = Objects.requireNonNull(type);
        this.value = value;
        this.targetX = targetX;
        this.targetY = targetY;
        this.landingTick = landingTick;
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
        long currentTick = world.game().getCurrentTick();

        return new Sun(
                world,
                SunSource.PLANT,
                Objects.requireNonNull(producer),
                SunType.NORMAL,
                value,
                x,
                y,
                currentTick,
                currentTick,
                SunState.AVAILABLE
        );
    }

    public static Sun fromSky(
            World world,
            SunType type,
            double targetX,
            double targetY,
            int value
    ) {
        long spawnTick = world.game().getCurrentTick();

        return new Sun(
                world,
                SunSource.SKY,
                null,
                type,
                value,
                targetX,
                targetY,
                spawnTick,
                spawnTick + SKY_FALL_DURATION_TICKS,
                SunState.FALLING
        );
    }

    @Override
    public void update(long tick) {
        if (state == SunState.FALLING) {
            updateFalling(tick);
        }
        else if (state == SunState.AVAILABLE) {
            updateAvailable();
        }
    }

    private void updateFalling(long tick) {
        if (tick < landingTick) {
            return;
        }

        state = SunState.AVAILABLE;
        availableTicksLeft = AVAILABLE_LIFETIME_TICKS;

        if (type == SunType.RADIOACTIVE) {
            type = SunType.NORMAL;
        }

        GameEvents.publish(
                "Sun reached the ground at position ("
                        + getTileX() + ", " + getTileY() + ")"
        );
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
        return targetX;
    }

    @Override
    public double getY() {
        return targetY;
    }

    public boolean isFalling() {
        return state == SunState.FALLING;
    }

    public boolean isRadioactiveWhileFalling() {
        return type == SunType.RADIOACTIVE
                && state == SunState.FALLING;
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
    
    public long getLandingTick() {
        return landingTick;
    }
}
