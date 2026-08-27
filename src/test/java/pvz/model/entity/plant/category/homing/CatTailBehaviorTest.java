package pvz.model.entity.plant.category.homing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pvz.data.PlantCsvLoader;
import pvz.data.PlantData;
import pvz.data.ZombieCsvLoader;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class CatTailBehaviorTest {

    private static final long FIRST_SHOT_TICKS = 15;

    private static final double CAT_TAIL_DAMAGE = 15;

    private Game game;
    private World world;
    private PlantData plantData;
    private PlantFactory plantFactory;
    private ZombieFactory zombieFactory;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        world = new World(
                game,
                new Board(9, 5),
                new BattleResources(1000, 0),
                new Random(23)
        );
        plantData = PlantCsvLoader.load("assets/Data/plants.csv");
        plantFactory = new PlantFactory(plantData.byName());
        zombieFactory = new ZombieFactory(
                ZombieCsvLoader.load("assets/Data/zombies.csv")
        );
        world.setZombieCreator(id -> zombieFactory.create(id, 3));
        game.register(world.board());
        GameEvents.drain();
    }

    @Test
    void allThreeHomingPlantsHaveRealBehaviorsAndPlantFood() {
        for (String name : java.util.List.of(
                "Caulipower",
                "Electric Blueberry",
                "Cat-tail"
        )) {
            assertNotNull(
                    plantFactory.create(name).behaviorCapability(
                            AbstractHomingBehavior.class
                    ),
                    name + " must not be passive"
            );
            assertTrue(
                    PlantFoodSupport.isImplemented(spec(name)),
                    name + " must support plant food"
            );
        }

        for (String name : java.util.List.of(
                "Magnet-shroom",
                "catTail-mint"
        )) {
            assertNull(
                    plantFactory.create(name).behaviorCapability(
                            AbstractHomingBehavior.class
                    ),
                    name + " must stay passive"
            );
            assertFalse(PlantFoodSupport.isImplemented(spec(name)));
        }
    }

    @Test
    void catTailAlwaysPicksTheZombieClosestToTheHouse() {
        Plant catTail = placePlant("Cat-tail", 6, 3);
        Zombie deepZombie = frozenZombieAt(3, 5, -0.3);
        Zombie nearZombie = frozenZombieAt(3, 3, 0.3);

        assertEquals(2.2, deepZombie.getX(), 1e-9);
        assertEquals(2.8, nearZombie.getX(), 1e-9);

        game.advance(FIRST_SHOT_TICKS + 30);

        assertTrue(damageTaken(deepZombie) >= CAT_TAIL_DAMAGE);
        assertEquals(0, damageTaken(nearZombie));
        assertFalse(catTail.isRemovedFromWorld());
    }

    @Test
    void catTailBreaksEqualAdvancementTiesByDistanceToItself() {
        placePlant("Cat-tail", 6, 3);
        Zombie farRow = frozenZombieAt(3, 5, -0.3);
        Zombie sameRow = frozenZombieAt(3, 3, -0.3);

        assertEquals(farRow.getX(), sameRow.getX(), 1e-9);

        game.advance(FIRST_SHOT_TICKS + 30);

        assertTrue(damageTaken(sameRow) >= CAT_TAIL_DAMAGE);
        assertEquals(0, damageTaken(farRow));
    }

    @Test
    void catTailNeverTargetsTerrainWhileAZombieIsAlive() {
        placePlant("Cat-tail", 6, 3);
        world.board().setTileType(2, 1, TileType.TOMBSTONE);
        Zombie zombie = frozenZombieAt(8, 3, 0);

        game.advance(FIRST_SHOT_TICKS + 40);

        assertEquals(700, world.board().getTile(2, 1).getHealth());
        assertTrue(damageTaken(zombie) >= CAT_TAIL_DAMAGE);
    }

    @Test
    void catTailFallsBackToDestructibleContentWithTheSamePriority() {
        placePlant("Cat-tail", 6, 3);
        world.board().setTileType(2, 1, TileType.TOMBSTONE);
        world.board().setTileType(8, 3, TileType.TOMBSTONE);

        game.advance(FIRST_SHOT_TICKS + 40);

        assertTrue(world.board().getTile(2, 1).getHealth() < 700);
        assertEquals(700, world.board().getTile(8, 3).getHealth());
    }

    @Test
    void catTailDamagesArmorThroughTheProjectilePath() {
        placePlant("Cat-tail", 6, 3);
        Zombie coneHead = frozenZombieAt("ZombieArmor1", 4, 3, 0);
        double armorBefore = coneHead.getArmorHealth();
        double healthBefore = coneHead.getHealth();

        game.advance(FIRST_SHOT_TICKS + 15);

        assertEquals(armorBefore - CAT_TAIL_DAMAGE, coneHead.getArmorHealth());
        assertEquals(healthBefore, coneHead.getHealth());
    }

    @Test
    void catTailPlantFoodFiresEightHomingSpikes() {
        Plant catTail = placePlant("Cat-tail", 6, 3);
        Zombie zombie = frozenZombieAt(7, 3, 0);
        double healthBefore = zombie.getHealth();

        assertTrue(catTail.tryApplyPlantFood(game.getCurrentTick()));

        game.advance(14);

        assertEquals(
                healthBefore - 8 * CAT_TAIL_DAMAGE,
                zombie.getHealth()
        );
    }

    @Test
    void catTailAlsoTargetsLivingPushedObstaclesWithoutZombies() {
        placePlant("Cat-tail", 6, 3);
        PushedObstacle obstacle = spawnIceBlock(2, 3);
        double healthBefore = obstacle.getHealth();

        game.advance(FIRST_SHOT_TICKS + 30);

        assertTrue(obstacle.getHealth() < healthBefore);
    }

    private double damageTaken(Zombie zombie) {
        return zombie.getMaximumHealth() - zombie.getHealth();
    }

    private pvz.model.entity.plant.PlantSpec spec(String name) {
        return plantData.byName().get(name.toLowerCase());
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        world.board().plant(column, row, plant);
        plant.place(world, column, row, game.getCurrentTick());
        game.register(plant);
        return plant;
    }

    private Zombie frozenZombieAt(int column, int row, double offsetX) {
        return frozenZombieAt("ZombieDefault", column, row, offsetX);
    }

    private Zombie frozenZombieAt(
            String id,
            int column,
            int row,
            double offsetX
    ) {
        Zombie zombie = zombieFactory.create(id, 3);
        zombie.spawn(world, column, row);

        if (offsetX != 0) {
            zombie.moveByTiles(offsetX);
        }

        zombie.applyFreeze(game.getCurrentTick(), 4000);
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
