package pvz.model.entity.collectible.sun;

import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.Updatable;
import pvz.model.core.World;

public final class SkySunSpawner implements Updatable {
    private static final int NORMAL_CHANCE_PERCENT = 80;
    private static final int SPECIAL_CHANCE_PERCENT = 15;
    /// RADIOACTIVE_CHALENCE_PERCENT = 5;

    private static final double BASE_INTERVAL_SECONDS = 6.0;
    private static final double MINIMUM_INTERVAL_SECONDS = 12.0;
    private static final double INTERVAL_GROWTH_PER_SECOND = 0.05;

    private final World world;
    private final Random random;

    private long nextDropTick;

    public SkySunSpawner(World world) {
        this(world, new Random());
    }

    SkySunSpawner(World world, Random random) { // Used a fixed seed for reproducible random values (for testing)
        this.world = Objects.requireNonNull(world);
        this.random = Objects.requireNonNull(random);

        nextDropTick = secondsToTicks(
                calculateIntervalSeconds(0)
        );
    }

    @Override
    public void update(long tick) {
        if (tick < nextDropTick) {
            return;
        }

        spawnSkySun();
        scheduleNextDrop(tick);
    }

    private void spawnSkySun() {
        int targetColumn =
                1 + random.nextInt(world.board().getCols());

        int targetRow =
                1 + random.nextInt(world.board().getRows());

        SunType type = rollSunType();
        int value = getSunValue(type);

        double targetX = targetColumn - 0.5;
        double targetY = targetRow - 0.5;


        double startY = Math.nextDown( // In method yek adad haddi nazdik rows mide(kamtar)
                (double) world.board().getRows()
        );

        Sun sun = Sun.fromSky(
                world,
                type,
                targetX,
                startY,
                targetY,
                value
        );

        world.addCollectible(sun);
        world.game().register(sun);

        GameEvents.publish(
                "New "
                        + type.name().toLowerCase(Locale.ROOT)
                        + " sun is dropping at position ("
                        + targetColumn + ", "
                        + targetRow + ")"
        );
    }

    private SunType rollSunType() {
        int roll = random.nextInt(100);

        if (roll < NORMAL_CHANCE_PERCENT) {
            return SunType.NORMAL;
        }

        if (roll < NORMAL_CHANCE_PERCENT + SPECIAL_CHANCE_PERCENT) {
            return SunType.SPECIAL;
        }

        return SunType.RADIOACTIVE;
    }

    private int getSunValue(SunType type) {
        return switch (type) {
            case NORMAL, RADIOACTIVE ->
                    SunValue.NORMALSUN.getValue();

            case SPECIAL ->
                    SunValue.SPECIALSUN.getValue();
        };
    }

    private void scheduleNextDrop(long currentTick) {
        double elapsedSeconds =
                currentTick / (double) Game.TICKS_PER_SECOND;

        double intervalSeconds =
                calculateIntervalSeconds(elapsedSeconds);

        nextDropTick =
                currentTick + secondsToTicks(intervalSeconds);
    }

    private double calculateIntervalSeconds(
            double elapsedSeconds
    ) {
        return Math.max(
                BASE_INTERVAL_SECONDS + INTERVAL_GROWTH_PER_SECOND * elapsedSeconds,
                MINIMUM_INTERVAL_SECONDS
        );
    }

    private long secondsToTicks(double seconds) {
        return (long) Math.ceil(
                seconds * Game.TICKS_PER_SECOND
        );
    }
}