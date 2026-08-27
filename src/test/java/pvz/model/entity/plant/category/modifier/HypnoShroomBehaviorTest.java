package pvz.model.entity.plant.category.modifier;

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
import pvz.data.ZombieCsvLoader;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.hit.PlantHitSource;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class HypnoShroomBehaviorTest {

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
                new Random(71)
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
    void nonMintModifiersAreImplementedForNow() {
        Plant hypno = plantFactory.create("Hypno-shroom");
        Plant torchwood = plantFactory.create("Torchwood");
        Plant lilyPad = plantFactory.create("Lily Pad");
        Plant imitater = plantFactory.create("Imitater");
        Plant mint = plantFactory.create("Enchant-mint");

        assertFalse(
                hypno.behaviorCapability(HypnoShroomStateCapability.class)
                        == null
        );
        assertNotNull(
                torchwood.behaviorCapability(TorchwoodStateCapability.class)
        );
        assertTrue(PlantFoodSupport.isImplemented(torchwood.getSpec()));
        assertTrue(PlantFoodSupport.isImplemented(lilyPad.getSpec()));
        assertTrue(PlantFoodSupport.isImplemented(imitater.getSpec()));
        assertFalse(PlantFoodSupport.isImplemented(mint.getSpec()));
        assertTrue(PlantFoodSupport.isImplemented(hypno.getSpec()));
    }

    @Test
    void normalHypnoShroomHypnotizesItsFirstBiterAndIsConsumed() {
        Plant hypno = placePlant("Hypno-shroom", 4, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 4, 3);

        game.advance(Game.TICKS_PER_SECOND + 1);

        assertTrue(zombie.isAllied());
        assertTrue(hypno.isRemovedFromWorld());
        assertFalse(world.getAlliedZombies().isEmpty());
        assertTrue(world.getHostileZombies().isEmpty());
    }

    @Test
    void nonBiteHitsNeverTriggerHypnosisOrTransformation() {
        Plant hypno = placePlant("Hypno-shroom", 4, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 5, 3);
        double initialHealth = hypno.getHealth();

        assertTrue(hypno.receiveHit(
                PlantHitSource.PROJECTILE,
                zombie,
                10
        ));
        assertTrue(zombie.isHostile());
        assertEquals(initialHealth - 10, hypno.getHealth());
        assertFalse(hypno.isRemovedFromWorld());

        assertTrue(hypno.tryApplyPlantFood(game.getCurrentTick()));
        game.advance(2L * Game.TICKS_PER_SECOND);

        assertTrue(hypno.receiveHit(
                PlantHitSource.PROJECTILE,
                zombie,
                10
        ));
        assertTrue(zombie.isHostile());
        assertFalse(hypno.isRemovedFromWorld());
        assertEquals(1, world.getHostileZombies().size());
        assertTrue(world.getAlliedZombies().isEmpty());
    }

    @Test
    void plantFoodPromotesHypnoToPermanentStageTwo() {
        Plant hypno = placePlant("Hypno-shroom", 4, 3);
        HypnoShroomStateCapability state = hypno.behaviorCapability(
                HypnoShroomStateCapability.class
        );

        assertNotNull(state);
        assertEquals(HypnoShroomStage.NORMAL, state.getStage());
        assertTrue(hypno.canApplyPlantFood(game.getCurrentTick()));
        assertTrue(hypno.tryApplyPlantFood(game.getCurrentTick()));
        assertEquals(HypnoShroomStage.GARGANTUAR_ARMED, state.getStage());
        assertFalse(hypno.canApplyPlantFood(game.getCurrentTick()));
        assertFalse(hypno.tryApplyPlantFood(game.getCurrentTick()));

        game.advance(3L * Game.TICKS_PER_SECOND);

        assertFalse(hypno.isPlantFoodActive(game.getCurrentTick()));
        assertEquals(HypnoShroomStage.GARGANTUAR_ARMED, state.getStage());
        assertFalse(hypno.canApplyPlantFood(game.getCurrentTick()));
        assertFalse(hypno.tryApplyPlantFood(game.getCurrentTick()));
    }

    @Test
    void stageTwoBlocksBitesDuringPlantFoodThenTransformsTheBiter() {
        Plant hypno = placePlant("Hypno-shroom", 4, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 4, 3);
        double plantHealth = hypno.getHealth();

        assertTrue(hypno.tryApplyPlantFood(game.getCurrentTick()));

        game.advance(12);

        assertTrue(zombie.isHostile());
        assertEquals(plantHealth, hypno.getHealth());
        assertFalse(hypno.isRemovedFromWorld());
        assertTrue(zombie.isEating());

        game.advance(10);

        assertTrue(hypno.isRemovedFromWorld());
        assertNull(zombie.getWorld());
        assertEquals(1, world.getAlliedZombies().size());
        Zombie gargantuar = world.getAlliedZombies().getFirst();
        assertEquals("Gargantuar", gargantuar.getName());
        assertEquals(
                gargantuar.getMaximumHealth(),
                gargantuar.getHealth()
        );
        assertTrue(world.getHostileZombies().isEmpty());
    }

    @Test
    void transformationPreservesPositionAndDoesNotCountAsDeath() {
        Plant hypno = placePlant("Hypno-shroom", 4, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 4, 3);
        zombie.moveByTiles(0.2);
        double originalX = zombie.getX();
        double originalY = zombie.getY();
        boolean originalGlowing = zombie.isGlowing();

        assertTrue(hypno.tryApplyPlantFood(game.getCurrentTick()));
        game.advance(21);

        assertNull(zombie.getWorld());
        assertFalse(zombie.isDead());
        assertTrue(world.getCollectibles().isEmpty());

        Zombie gargantuar = world.getAlliedZombies().getFirst();
        assertEquals(originalX, gargantuar.getX());
        assertEquals(originalY, gargantuar.getY());
        assertEquals(originalGlowing, gargantuar.isGlowing());
        assertTrue(
                GameEvents.drain().stream()
                        .noneMatch(message -> message.contains(" is dead at "))
        );
    }

    @Test
    void transformationPreservesGlowingWithoutConsumingExtraRandomRoll() {
        CountingRandom random = new CountingRandom(71);
        Game isolatedGame = new Game();
        World isolatedWorld = new World(
                isolatedGame,
                new Board(9, 5),
                new BattleResources(1000, 0),
                random
        );
        isolatedWorld.setZombieCreator(
                id -> zombieFactory.create(id, 3)
        );
        isolatedGame.register(isolatedWorld.board());

        Plant hypno = plantFactory.create("Hypno-shroom");
        isolatedWorld.board().plant(4, 3, hypno);
        hypno.place(isolatedWorld, 4, 3, isolatedGame.getCurrentTick());
        isolatedGame.register(hypno);

        Zombie zombie = zombieFactory.create("ZombieDefault", 3);
        zombie.spawn(isolatedWorld, 4, 3);
        int rollsAfterInitialSpawn = random.nextDoubleCalls();
        boolean originalGlowing = zombie.isGlowing();

        assertTrue(hypno.tryApplyPlantFood(isolatedGame.getCurrentTick()));
        isolatedGame.advance(21);

        assertEquals(rollsAfterInitialSpawn, random.nextDoubleCalls());
        Zombie gargantuar = isolatedWorld.getAlliedZombies().getFirst();
        assertEquals(originalGlowing, gargantuar.isGlowing());
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

    private static final class CountingRandom extends Random {

        private int nextDoubleCalls;

        CountingRandom(long seed) {
            super(seed);
        }

        @Override
        public double nextDouble() {
            nextDoubleCalls++;
            return super.nextDouble();
        }

        int nextDoubleCalls() {
            return nextDoubleCalls;
        }
    }

}
