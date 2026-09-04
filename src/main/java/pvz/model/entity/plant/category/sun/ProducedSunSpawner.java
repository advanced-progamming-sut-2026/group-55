package pvz.model.entity.plant.category.sun;

import java.util.Objects;

import pvz.model.core.World;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.plant.Plant;

public final class ProducedSunSpawner {
    private static final double SPAWN_RADIUS_TILES = 0.9;
    private static final int MAX_RANDOM_POSITION_ATTEMPTS = 64;
    private static final double FULL_CIRCLE_RADIANS = Math.PI * 2.0;

    private ProducedSunSpawner() {
    }

    public static Sun spawnAtProducer(
            World world,
            Plant producer,
            int value
    ) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(producer, "producer plant cannot be null");

        if (value <= 0) {
            throw new IllegalArgumentException(
                    "produced sun value must be positive"
            );
        }

        SpawnPosition position = randomSpawnPosition(world, producer);
        Sun sun = Sun.fromPlant(
                world,
                producer,
                position.x(),
                position.y(),
                value
        );

        world.addCollectible(sun);
        world.game().register(sun);

        return sun;
    }

    private static SpawnPosition randomSpawnPosition(
            World world,
            Plant producer
    ) {
        double centerX = producer.getX();
        double centerY = producer.getY();

        for (int attempt = 0; attempt < MAX_RANDOM_POSITION_ATTEMPTS; attempt++) {
            SpawnPosition candidate = randomPointInCircle(
                    world, centerX, centerY
            );
            if (isInsideBoard(world, candidate)) {
                return candidate;
            }
        }

        return new SpawnPosition(centerX, centerY);
    }

    private static SpawnPosition randomPointInCircle(
            World world,
            double centerX,
            double centerY
    ) {
        double angle = world.randomDouble() * FULL_CIRCLE_RADIANS;
        double distance = SPAWN_RADIUS_TILES
                * Math.sqrt(world.randomDouble());

        return new SpawnPosition(
                centerX + Math.cos(angle) * distance,
                centerY + Math.sin(angle) * distance
        );
    }

    private static boolean isInsideBoard(
            World world,
            SpawnPosition position
    ) {
        return position.x() >= 0.0
                && position.x() < world.board().getCols()
                && position.y() >= 0.0
                && position.y() < world.board().getRows();
    }

    private record SpawnPosition(double x, double y) {
    }
}
