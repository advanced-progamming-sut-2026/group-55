package pvz.model.entity.plant.category.sun;

import java.util.Objects;

import pvz.model.core.World;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.plant.Plant;

public final class ProducedSunSpawner {

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

        Sun sun = Sun.fromPlant(
                world,
                producer,
                producer.getX(),
                producer.getY(),
                value
        );

        world.addCollectible(sun);
        world.game().register(sun);

        return sun;
    }
}
