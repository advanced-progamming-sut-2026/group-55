package pvz.model.entity.plant.category.sun;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;

class ProducedSunSpawnerRandomPositionTest {
    private static final double RADIUS = 0.9;
    private static final double EPSILON = 0.000001;

    @Test
    void everyProducedSunGetsAnIndependentPositionInsideTheCircle() {
        Game game = new Game();
        World world = world(game, 9, 5, 14L);
        Plant producer = placedSunflower(world, 5, 3);

        Sun first = ProducedSunSpawner.spawnAtProducer(world, producer, 50);
        Sun second = ProducedSunSpawner.spawnAtProducer(world, producer, 50);

        assertWithinRadius(producer, first);
        assertWithinRadius(producer, second);
        assertNotEquals(first.getTargetX(), second.getTargetX(), EPSILON);
        assertNotEquals(first.getTargetY(), second.getTargetY(), EPSILON);
    }

    @Test
    void repeatedSpawnsAlwaysKeepTheirCentersInsideTheBoard() {
        Game game = new Game();
        World world = world(game, 9, 5, 7L);
        Plant producer = placedSunflower(world, 1, 1);

        for (int index = 0; index < 250; index++) {
            Sun sun = ProducedSunSpawner.spawnAtProducer(
                    world, producer, 25
            );
            assertWithinRadius(producer, sun);
            assertTrue(sun.getTargetX() >= 0.0);
            assertTrue(sun.getTargetX() < world.board().getCols());
            assertTrue(sun.getTargetY() >= 0.0);
            assertTrue(sun.getTargetY() < world.board().getRows());
        }
    }

    @Test
    void generatedDistanceUsesTheFullRequestedRadius() {
        Game game = new Game();
        World world = world(game, 9, 5, 99L);
        Plant producer = placedSunflower(world, 5, 3);
        double largestDistance = 0.0;

        for (int index = 0; index < 400; index++) {
            Sun sun = ProducedSunSpawner.spawnAtProducer(
                    world, producer, 50
            );
            largestDistance = Math.max(
                    largestDistance, distance(producer, sun)
            );
        }

        assertTrue(largestDistance > 0.8);
        assertTrue(largestDistance <= RADIUS + EPSILON);
    }

    private World world(Game game, int columns, int rows, long seed) {
        return new World(
                game,
                new Board(columns, rows),
                new BattleResources(0, 0),
                new Random(seed)
        );
    }

    private Plant placedSunflower(World world, int column, int row) {
        Plant plant = new Plant(new PlantSpec(
                1,
                "Sunflower",
                PlantCategory.SUN_PRODUCER,
                Set.of(),
                50,
                300,
                "0",
                "produce sun",
                "",
                "",
                "",
                "",
                24.0,
                5.0
        ));
        world.board().plant(column, row, plant);
        plant.place(world, column, row, world.game().getCurrentTick());
        return plant;
    }

    private void assertWithinRadius(Plant producer, Sun sun) {
        assertTrue(distance(producer, sun) <= RADIUS + EPSILON);
    }

    private double distance(Plant producer, Sun sun) {
        double dx = sun.getTargetX() - producer.getX();
        double dy = sun.getTargetY() - producer.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
