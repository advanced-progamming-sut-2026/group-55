package pvz.model.entity.plant.category.explosive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
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
import pvz.model.core.board.TileOverlayType;
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class ExplosiveSupportPlantsTest {

    private static final long CRATER_TICKS = 1800;

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
                new Random(41)
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
    void jalapenoBurnsOnlyItsOwnRowAndMeltsThatRowsIce() {
        Plant frozenPlant = placePlant("Peashooter", 3, 3);
        Plant safePlant = placePlant("Peashooter", 7, 3);
        for (int level = 0; level < Plant.FULL_FREEZE_LEVEL; level++) {
            world.board().addPlantFreezeLevel(
                    frozenPlant,
                    Plant.FULL_FREEZE_LEVEL
            );
        }
        PushedObstacle iceBlock = spawnIceBlock(6, 3);
        Zombie sameRowNear = frozenZombie("ZombieDefault", 2, 3);
        Zombie sameRowFar = frozenZombie("ZombieDefault", 8, 3);
        Zombie otherRow = frozenZombie("ZombieDefault", 5, 2);
        double safeHealth = safePlant.getHealth();

        placePlant("Jalapeno", 5, 3);
        game.advance(5);

        assertTrue(sameRowNear.isDead());
        assertTrue(sameRowFar.isDead());
        assertFalse(otherRow.isDead());
        assertTrue(iceBlock.isDead());
        assertFalse(world.board().getTile(3, 3)
                .hasOverlay(TileOverlayType.FROZEN));
        assertEquals(0, frozenPlant.getFreezeLevel());
        assertEquals(safeHealth, safePlant.getHealth());
    }


    @Test
    void jalapenoClearsFreezeAndChillOnlyFromItsOwnRow() {
        Zombie sameRow = spawnZombie("ZombieGargantuar", 8, 3);
        Zombie otherRow = spawnZombie("ZombieGargantuar", 8, 2);
        long tick = game.getCurrentTick();

        sameRow.applyChill(tick, 4000);
        sameRow.applyFreeze(tick, 4000);
        otherRow.applyChill(tick, 4000);
        otherRow.applyFreeze(tick, 4000);

        placePlant("Jalapeno", 5, 3);
        game.advance(5);
        long resolvedTick = game.getCurrentTick();

        assertFalse(sameRow.isChilled(resolvedTick));
        assertFalse(sameRow.isFrozen(resolvedTick));
        assertTrue(otherRow.isChilled(resolvedTick));
        assertTrue(otherRow.isFrozen(resolvedTick));
    }

    @Test
    void doomShroomHitsTheWholeLawnWithoutTouchingPlants() {
        Plant survivor = placePlant("Peashooter", 1, 1);
        Zombie firstZombie = frozenZombie("ZombieDefault", 9, 1);
        Zombie secondZombie = frozenZombie("ZombieDefault", 2, 5);
        world.board().setTileType(4, 4, TileType.TOMBSTONE);
        double survivorHealth = survivor.getHealth();

        placePlant("Doom-shroom", 5, 3);
        game.advance(5);

        assertTrue(firstZombie.isDead());
        assertTrue(secondZombie.isDead());
        assertEquals(
                TileType.NORMAL,
                world.board().getTile(4, 4).getType()
        );
        assertEquals(survivorHealth, survivor.getHealth());
        assertTrue(world.board().hasCrater(5, 3));
        assertFalse(world.board().hasCrater(4, 3));
        assertFalse(world.board().hasCrater(5, 4));
    }

    @Test
    void doomShroomCraterBlocksPlantingUntilItExpires() {
        world.board().setTileType(5, 3, TileType.LOW_BEACH);
        Plant doomShroom = placePlant("Doom-shroom", 5, 3);

        game.advance(5);

        assertTrue(doomShroom.isRemovedFromWorld());
        assertTrue(world.board().hasCrater(5, 3));

        Plant blockedPlant = plantFactory.create("Peashooter");
        assertFalse(
                world.board().plant(5, 3, blockedPlant)
                        .startsWith("planted ")
        );

        Plant movable = placePlant("Peashooter", 4, 3);
        assertFalse(world.board().movePlant(4, 3, 5, 3, movable));

        game.advance(CRATER_TICKS);

        assertFalse(world.board().hasCrater(5, 3));
        assertEquals(
                TileType.LOW_BEACH,
                world.board().getTile(5, 3).getType()
        );

        Plant allowedPlant = plantFactory.create("Peashooter");
        assertTrue(
                world.board().plant(5, 3, allowedPlant)
                        .startsWith("planted ")
        );
    }

    @Test
    void tangleKelpTargetsOnlyZombiesInsideTheWater() {
        world.board().setTileType(8, 3, TileType.WATER);
        Plant kelp = placePlant("Tangle Kelp", 8, 3);
        PushedObstacle obstacle = spawnIceBlock(8, 3);

        game.advance(1);

        assertEquals(
                TransientActionWindow.State.IDLE,
                window(kelp).getState()
        );
        assertFalse(obstacle.isDead());

        Zombie zombie = frozenZombie("ZombieDefault", 8, 3);

        game.advance(1);

        assertTrue(zombie.isDead());
        assertTrue(window(kelp).isEffectActive());

        game.advance(5);

        assertTrue(kelp.isRemovedFromWorld());
    }

    @Test
    void tangleKelpPlantFoodDragsThreeRandomWaterZombies() {
        for (int row = 1; row <= 4; row++) {
            world.board().setTileType(7, row, TileType.WATER);
        }
        world.board().setTileType(8, 5, TileType.WATER);
        Plant kelp = placePlant("Tangle Kelp", 8, 5);

        List<Zombie> waterZombies = List.of(
                frozenZombie("ZombieDefault", 7, 1),
                frozenZombie("ZombieDefault", 7, 2),
                frozenZombie("ZombieDefault", 7, 3),
                frozenZombie("ZombieDefault", 7, 4)
        );
        Zombie landZombie = frozenZombie("ZombieDefault", 3, 1);

        assertTrue(kelp.tryApplyPlantFood(game.getCurrentTick()));

        assertEquals(
                3,
                waterZombies.stream().filter(Zombie::isDead).count()
        );
        assertFalse(landZombie.isDead());
    }

    @Test
    void icebergLettuceFreezesOnlyZombiesThatStepOnIt() {
        Plant iceberg = placePlant("Iceberg Lettuce", 5, 3);
        PushedObstacle obstacle = spawnIceBlock(5, 3);

        game.advance(1);

        assertEquals(
                TransientActionWindow.State.IDLE,
                window(iceberg).getState()
        );
        assertEquals(600, obstacle.getHealth());

        Zombie zombie = spawnZombie("ZombieDefault", 5, 3);

        game.advance(1);

        assertTrue(zombie.isFrozen(game.getCurrentTick()));
        assertFalse(zombie.isDead());
        assertEquals(600, obstacle.getHealth());
        assertTrue(window(iceberg).isEffectActive());
    }

    @Test
    void icebergPlantFoodFreezesEveryZombieOfTheLawn() {
        Plant iceberg = placePlant("Iceberg Lettuce", 2, 2);
        List<Zombie> zombies = List.of(
                spawnZombie("ZombieDefault", 9, 1),
                spawnZombie("ZombieDefault", 8, 3),
                spawnZombie("ZombieDefault", 7, 5)
        );

        assertTrue(iceberg.tryApplyPlantFood(game.getCurrentTick()));

        for (Zombie zombie : zombies) {
            assertTrue(zombie.isFrozen(game.getCurrentTick()));
        }
    }

    @Test
    void iceShroomFreezesEveryZombieWithoutDamagingAnythingElse() {
        Plant plant = placePlant("Peashooter", 1, 1);
        PushedObstacle obstacle = spawnIceBlock(4, 4);
        List<Zombie> zombies = List.of(
                spawnZombie("ZombieDefault", 9, 1),
                spawnZombie("ZombieDefault", 8, 4)
        );
        double plantHealth = plant.getHealth();

        Plant iceShroom = placePlant("Ice-shroom", 5, 3);
        game.advance(5);

        for (Zombie zombie : zombies) {
            assertTrue(zombie.isFrozen(game.getCurrentTick()));
            assertFalse(zombie.isDead());
        }
        assertEquals(600, obstacle.getHealth());
        assertEquals(plantHealth, plant.getHealth());
        assertTrue(iceShroom.isRemovedFromWorld());
    }

    @Test
    void hotPotatoIsOnlyValidOnFrozenTilesAndMeltsTheIce() {
        Plant frozenPlant = placePlant("Peashooter", 4, 2);
        for (int level = 0; level < Plant.FULL_FREEZE_LEVEL; level++) {
            world.board().addPlantFreezeLevel(
                    frozenPlant,
                    Plant.FULL_FREEZE_LEVEL
            );
        }
        double frozenHealth = frozenPlant.getHealth();

        Plant rejected = plantFactory.create("Hot Potato");
        assertFalse(
                world.board().plant(5, 2, rejected).startsWith("planted ")
        );
        assertTrue(world.board().getTile(5, 2).getPlants().isEmpty());

        Plant hotPotato = placePlant("Hot Potato", 4, 2);

        assertTrue(world.board().getTile(4, 2)
                .hasOverlay(TileOverlayType.FROZEN));

        game.advance(5);

        assertFalse(world.board().getTile(4, 2)
                .hasOverlay(TileOverlayType.FROZEN));
        assertEquals(0, frozenPlant.getFreezeLevel());
        assertEquals(frozenHealth, frozenPlant.getHealth());
        assertTrue(hotPotato.isRemovedFromWorld());
        assertFalse(frozenPlant.isRemovedFromWorld());
        assertEquals(
                List.of(frozenPlant),
                world.board().getTile(4, 2).getPlants()
        );
    }

    @Test
    void graveBusterIsOnlyValidOnTombstonesAndDestroysThem() {
        world.board().setTileType(6, 4, TileType.TOMBSTONE);

        Plant rejected = plantFactory.create("Grave Buster");
        assertFalse(
                world.board().plant(6, 3, rejected).startsWith("planted ")
        );

        Plant graveBuster = placePlant("Grave Buster", 6, 4);

        assertEquals(
                TileType.TOMBSTONE,
                world.board().getTile(6, 4).getType()
        );

        game.advance(20);

        assertEquals(
                TileType.NORMAL,
                world.board().getTile(6, 4).getType()
        );
        assertTrue(graveBuster.isRemovedFromWorld());
        assertTrue(world.board().getTile(6, 4).getPlants().isEmpty());
    }

    private TransientActionWindow window(Plant plant) {
        return plant.behaviorCapability(AbstractExplosiveBehavior.class)
                .window();
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        world.board().plant(column, row, plant);
        plant.place(world, column, row, game.getCurrentTick());
        game.register(plant);
        return plant;
    }

    private Zombie spawnZombie(String id, int column, int row) {
        Zombie zombie = zombieFactory.create(id, 3);
        zombie.spawn(world, column, row);
        return zombie;
    }

    private Zombie frozenZombie(String id, int column, int row) {
        Zombie zombie = spawnZombie(id, column, row);
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
