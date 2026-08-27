package pvz.model.entity.plant.category.explosive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pvz.data.PlantCsvLoader;
import pvz.data.ZombieCsvLoader;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class SquashBehaviorTest {

    private Game game;
    private World world;
    private PlantFactory plantFactory;
    private ZombieFactory zombieFactory;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        world = new World(
                game,
                new Board(9, 5),
                new BattleResources(1000, 0),
                new Random(21)
        );
        plantFactory = new PlantFactory(
                PlantCsvLoader.load("assets/Data/plants.csv").byName()
        );
        zombieFactory = new ZombieFactory(
                ZombieCsvLoader.load("assets/Data/zombies.csv")
        );
        world.setZombieCreator(id -> zombieFactory.create(id, 3));
        game.register(world.board());
        GameEvents.drain();
    }

    @Test
    void squashPrefersItsOwnTileOverForwardAndBackwardTiles() {
        Plant squash = placePlant("Squash", 5, 3);
        world.board().setTileType(6, 3, TileType.TOMBSTONE);
        world.board().setTileType(4, 3, TileType.TOMBSTONE);
        Zombie zombie = frozenZombie(5, 3);

        game.advance(1);

        assertFalse(zombie.isDead());
        assertEquals(SquashDirection.CENTER, targeting(squash).getLastDirection());
        assertEquals(5, targeting(squash).getLastTargetColumn());
        assertEquals(3, targeting(squash).getLastTargetRow());

        game.advance(5);

        assertTrue(zombie.isDead());
        assertEquals(
                TileType.TOMBSTONE,
                world.board().getTile(6, 3).getType()
        );
        assertEquals(
                TileType.TOMBSTONE,
                world.board().getTile(4, 3).getType()
        );
    }

    @Test
    void squashPrefersTheForwardTileOverTheBackwardTile() {
        Plant squash = placePlant("Squash", 5, 3);
        world.board().setTileType(6, 3, TileType.TOMBSTONE);
        world.board().setTileType(4, 3, TileType.TOMBSTONE);

        game.advance(1);

        assertEquals(
                SquashDirection.FORWARD,
                targeting(squash).getLastDirection()
        );
        assertEquals(6, targeting(squash).getLastTargetColumn());

        game.advance(5);

        assertEquals(
                TileType.NORMAL,
                world.board().getTile(6, 3).getType()
        );
        assertEquals(
                TileType.TOMBSTONE,
                world.board().getTile(4, 3).getType()
        );
    }

    @Test
    void squashUsesTheBackwardTileWhenItIsTheOnlyTarget() {
        Plant squash = placePlant("Squash", 5, 3);
        world.board().setTileType(4, 3, TileType.TOMBSTONE);

        game.advance(1);

        assertEquals(
                SquashDirection.BACKWARD,
                targeting(squash).getLastDirection()
        );
        assertEquals(4, targeting(squash).getLastTargetColumn());

        game.advance(5);

        assertEquals(
                TileType.NORMAL,
                world.board().getTile(4, 3).getType()
        );
    }

    @Test
    void squashCrushesEveryNonFriendlyContentOfTheTargetTile() {
        Plant squash = placePlant("Squash", 5, 3);
        Plant neighbourPlant = placePlant("Peashooter", 5, 2);
        world.board().setTileType(6, 3, TileType.TOMBSTONE);
        Zombie zombie = frozenZombie(6, 3);
        PushedObstacle obstacle = spawnIceBlock(6, 3);
        double neighbourHealth = neighbourPlant.getHealth();

        game.advance(1);

        assertEquals(
                SquashDirection.FORWARD,
                targeting(squash).getLastDirection()
        );
        assertFalse(zombie.isDead());

        game.advance(5);

        assertTrue(zombie.isDead());
        assertTrue(obstacle.isDead());
        assertEquals(
                TileType.NORMAL,
                world.board().getTile(6, 3).getType()
        );
        assertEquals(neighbourHealth, neighbourPlant.getHealth());
        assertTrue(squash.isRemovedFromWorld());
    }

    @Test
    void squashPlantFoodCrushesTwoRandomZombies() {
        Plant squash = placePlant("Squash", 2, 5);
        Zombie first = frozenZombie(8, 1);
        Zombie second = frozenZombie(8, 2);
        Zombie third = frozenZombie(8, 3);

        assertTrue(squash.tryApplyPlantFood(game.getCurrentTick()));

        long crushed = java.util.stream.Stream.of(first, second, third)
                .filter(Zombie::isDead)
                .count();

        assertEquals(2, crushed);
    }

    private SquashTargeting targeting(Plant squash) {
        return squash.behaviorCapability(SquashTargeting.class);
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        world.board().plant(column, row, plant);
        plant.place(world, column, row, game.getCurrentTick());
        game.register(plant);
        return plant;
    }

    private Zombie frozenZombie(int column, int row) {
        Zombie zombie = zombieFactory.create("ZombieDefault", 3);
        zombie.spawn(world, column, row);
        zombie.applyFreeze(game.getCurrentTick(), 400);
        return zombie;
    }

    private PushedObstacle spawnIceBlock(int column, int row) {
        PushedObstacle obstacle = new PushedObstacle(
                "ICE_BLOCK",
                "Ice Block",
                600,
                true,
                true,
                true
        );
        obstacle.spawn(world, column - 0.5, row - 0.5);
        game.register(obstacle);
        return obstacle;
    }
}
