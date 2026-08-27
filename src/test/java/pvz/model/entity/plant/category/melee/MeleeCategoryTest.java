package pvz.model.entity.plant.category.melee;

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
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class MeleeCategoryTest {

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
                new BattleResources(5000, 3),
                new Random(7)
        );
        plantFactory = new PlantFactory(
                PlantCsvLoader.load("assets/Data/plants.csv").byName()
        );
        zombieFactory = new ZombieFactory(
                ZombieCsvLoader.load("assets/Data/zombies.csv")
        );
        world.setZombieCreator(id -> zombieFactory.create(id, 3));
        game.register(world.board());
    }

    @Test
    void meleePlantsUseDedicatedBehaviorsAndSupportPlantFood() {
        for (String name : new String[]{
                "Bonk Choy",
                "Phat Beet",
                "Chomper",
                "Wasabi Whip",
                "Kiwibeast"
        }) {
            Plant plant = plantFactory.create(name);
            assertNotNull(
                    plant.behaviorCapability(MeleeVisualStateCapability.class),
                    name
            );
            assertTrue(PlantFoodSupport.isImplemented(plant.getSpec()), name);
        }
    }

    @Test
    void bonkChoyUsesContactDamageAndArmorAbsorbsIt() {
        placePlant("Bonk Choy", 4, 3);
        Zombie zombie = spawnZombie("ZombieArmor2", 5, 3);
        double health = zombie.getHealth();
        double armor = zombie.getArmorHealth();

        game.advance(3);

        assertEquals(health, zombie.getHealth());
        assertEquals(armor - 15, zombie.getArmorHealth());
    }

    @Test
    void bonkChoyPreservesItsQuarterSecondAverageCadence() {
        placePlant("Bonk Choy", 4, 3);
        Zombie zombie = spawnZombie("ZombieArmor2", 5, 3);
        zombie.applyFreeze(0, 20);
        double armor = zombie.getArmorHealth();

        game.advance(10);

        assertEquals(armor - 60, zombie.getArmorHealth());
    }

    @Test
    void phatBeetHitsOnlyZombiesInsideThreeByThreeArea() {
        placePlant("Phat Beet", 4, 3);
        Zombie near = spawnZombie("ZombieDefault", 5, 4);
        Zombie far = spawnZombie("ZombieDefault", 7, 3);
        double nearHealth = near.getHealth();
        double farHealth = far.getHealth();

        game.advance(20);

        assertEquals(nearHealth - 15, near.getHealth());
        assertEquals(farHealth, far.getHealth());
    }

    @Test
    void chomperCanSwallowImmediatelyThenWaitsForDigestion() {
        placePlant("Chomper", 4, 3);
        Zombie first = spawnZombie("ZombieArmor2", 5, 3);

        game.advance(1);

        assertTrue(first.isDead());

        Zombie second = spawnZombie("ZombieDefault", 5, 3);
        second.applyFreeze(game.getCurrentTick(), 500);

        game.advance(399);
        assertFalse(second.isDead());

        game.advance(1);
        assertTrue(second.isDead());
    }

    @Test
    void wasabiWhipRemovesColdEffectsOnSuccessfulHit() {
        placePlant("Wasabi Whip", 4, 3);
        Zombie zombie = spawnZombie("ZombieGargantuar", 5, 3);
        zombie.applyChill(0, 100);
        zombie.applyFreeze(0, 100);

        game.advance(20);

        assertFalse(zombie.isChilled(game.getCurrentTick()));
        assertFalse(zombie.isFrozen(game.getCurrentTick()));
    }

    @Test
    void kiwibeastGrowthUsesConfiguredStageTimes() {
        Plant plant = placePlant("Kiwibeast", 4, 3);
        MeleeVisualStateCapability state = plant.behaviorCapability(
                MeleeVisualStateCapability.class
        );

        assertEquals(1, state.getGrowthStage(0));
        assertEquals(2, state.getGrowthStage(240));
        assertEquals(3, state.getGrowthStage(720));
    }


    @Test
    void enforceMintRemainsUnsupportedAndPassiveForNow() {
        Plant mint = plantFactory.create("Enforce-mint");

        assertNull(
                mint.behaviorCapability(MeleeVisualStateCapability.class)
        );
        assertFalse(PlantFoodSupport.isImplemented(mint.getSpec()));
    }

    @Test
    void kiwibeastPlantFoodImmediatelyPromotesItToStageThree() {
        Plant plant = placePlant("Kiwibeast", 4, 3);
        MeleeVisualStateCapability state = plant.behaviorCapability(
                MeleeVisualStateCapability.class
        );

        assertEquals(1, state.getGrowthStage(game.getCurrentTick()));
        assertTrue(plant.tryApplyPlantFood(game.getCurrentTick()));

        assertEquals(3, state.getGrowthStage(game.getCurrentTick()));
        assertEquals(3, state.getGrowthStage(
                game.getCurrentTick() + 1_000
        ));
    }

    @Test
    void kiwibeastPlantFoodUsesStageThreeDamageImmediately() {
        Plant plant = placePlant("Kiwibeast", 4, 3);
        Zombie zombie = spawnZombie("ZombieGargantuar", 5, 3);
        double healthBefore = zombie.getHealth();

        assertTrue(plant.tryApplyPlantFood(game.getCurrentTick()));

        assertEquals(
                healthBefore - 180,
                zombie.getHealth()
        );
    }

    @Test
    void chomperDoesNotDigestWhenItsSwallowIsRejected() {
        Plant chomper = placePlant("Chomper", 4, 3);
        world.board().setTileType(5, 3, TileType.WATER);
        Zombie snorkel = spawnZombie("ZombieBeachSnorkel", 5, 3);
        MeleeVisualStateCapability state = chomper.behaviorCapability(
                MeleeVisualStateCapability.class
        );

        game.advance(1);

        assertFalse(snorkel.isDead());
        assertFalse(state.isDigesting(game.getCurrentTick()));
    }

    @Test
    void chomperPlantFoodDoesNotDigestWhenNothingWasSwallowed() {
        Plant chomper = placePlant("Chomper", 4, 3);
        world.board().setTileType(5, 3, TileType.WATER);
        Zombie snorkel = spawnZombie("ZombieBeachSnorkel", 5, 3);
        MeleeVisualStateCapability state = chomper.behaviorCapability(
                MeleeVisualStateCapability.class
        );

        assertTrue(chomper.tryApplyPlantFood(game.getCurrentTick()));

        assertFalse(snorkel.isDead());
        assertFalse(state.isDigesting(game.getCurrentTick()));
    }

    @Test
    void rejectedWasabiPlantFoodHitDoesNotClearColdEffects() {
        Plant wasabi = placePlant("Wasabi Whip", 4, 3);
        world.board().setTileType(5, 3, TileType.WATER);
        Zombie snorkel = spawnZombie("ZombieBeachSnorkel", 5, 3);
        snorkel.applyChill(0, 100);
        snorkel.applyFreeze(0, 100);

        assertTrue(wasabi.tryApplyPlantFood(game.getCurrentTick()));

        assertTrue(snorkel.isChilled(game.getCurrentTick()));
        assertTrue(snorkel.isFrozen(game.getCurrentTick()));
    }

    @Test
    void directionalMeleeUsesActualPositionForAttackDirection() {
        Plant bonk = placePlant("Bonk Choy", 4, 3);
        Zombie zombie = spawnZombie("ZombieArmor2", 4, 3);
        zombie.moveByTiles(-0.2);
        zombie.applyFreeze(0, 20);
        MeleeVisualStateCapability state = bonk.behaviorCapability(
                MeleeVisualStateCapability.class
        );

        game.advance(3);

        assertEquals(
                MeleeAttackDirection.BACKWARD,
                state.getLastAttackDirection()
        );
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        String result = world.board().plant(column, row, plant);
        assertTrue(result.startsWith("planted"), result);
        plant.place(world, column, row, game.getCurrentTick());
        if (!plant.isRemovedFromWorld()) {
            game.register(plant);
        }
        return plant;
    }

    private Zombie spawnZombie(String id, int column, int row) {
        return world.spawnZombie(id, column, row);
    }
}
