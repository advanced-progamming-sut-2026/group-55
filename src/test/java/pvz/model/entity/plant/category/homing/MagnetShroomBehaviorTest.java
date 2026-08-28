package pvz.model.entity.plant.category.homing;

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
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.HypnosisService;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class MagnetShroomBehaviorTest {

    private static final long MAGNET_INTERVAL_TICKS = 100;

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
                new Random(61)
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
    void normalActionRemovesBucketFromNearestEligibleZombieOnly() {
        placePlant("Magnet-shroom", 6, 3);
        Zombie fartherBucket = frozenZombie("ZombieArmor2", 2, 5);
        Zombie nearestBucket = frozenZombie("ZombieArmor2", 5, 3);
        Zombie cone = frozenZombie("ZombieArmor1", 4, 3);

        double nearestHealth = nearestBucket.getHealth();
        double fartherArmor = fartherBucket.getArmorHealth();
        double coneArmor = cone.getArmorHealth();

        game.advance(MAGNET_INTERVAL_TICKS);

        assertFalse(nearestBucket.getArmorSet().hasArmor("BUCKET"));
        assertEquals(0, nearestBucket.getArmorHealth());
        assertEquals(nearestHealth, nearestBucket.getHealth());
        assertEquals(fartherArmor, fartherBucket.getArmorHealth());
        assertEquals(coneArmor, cone.getArmorHealth());
    }

    @Test
    void knightLosesCrownButKeepsShoulderArmor() {
        placePlant("Magnet-shroom", 6, 3);
        Zombie knight = frozenZombie("ZombieDarkArmor3", 4, 3);
        double healthBefore = knight.getHealth();

        assertTrue(knight.getArmorSet().hasArmor("CROWN"));
        assertTrue(knight.getArmorSet().hasArmor("SHOULDER_ARMOR"));

        game.advance(MAGNET_INTERVAL_TICKS);

        assertFalse(knight.getArmorSet().hasArmor("CROWN"));
        assertTrue(knight.getArmorSet().hasArmor("SHOULDER_ARMOR"));
        assertEquals(1600, knight.getArmorHealth());
        assertEquals(healthBefore, knight.getHealth());
    }

    @Test
    void missingTargetDoesNotConsumeTheNormalCooldown() {
        placePlant("Magnet-shroom", 6, 3);
        frozenZombie("ZombieArmor1", 4, 3);

        game.advance(MAGNET_INTERVAL_TICKS + 20);

        Zombie bucket = frozenZombie("ZombieArmor2", 5, 3);
        game.advance(1);

        assertFalse(bucket.getArmorSet().hasArmor("BUCKET"));
    }

    @Test
    void plantFoodDisarmsEveryHostileEligibleZombieOnce() {
        Plant magnet = placePlant("Magnet-shroom", 6, 3);
        Zombie bucketOne = frozenZombie("ZombieArmor2", 2, 1);
        Zombie bucketTwo = frozenZombie("ZombieArmor2", 3, 5);
        Zombie knight = frozenZombie("ZombieDarkArmor3", 4, 2);
        Zombie cone = frozenZombie("ZombieArmor1", 5, 4);
        Zombie alliedBucket = frozenZombie("ZombieArmor2", 2, 3);
        HypnosisService.hypnotize(alliedBucket, game.getCurrentTick());

        assertTrue(magnet.tryApplyPlantFood(game.getCurrentTick()));

        assertFalse(bucketOne.getArmorSet().hasArmor("BUCKET"));
        assertFalse(bucketTwo.getArmorSet().hasArmor("BUCKET"));
        assertFalse(knight.getArmorSet().hasArmor("CROWN"));
        assertTrue(knight.getArmorSet().hasArmor("SHOULDER_ARMOR"));
        assertTrue(cone.getArmorSet().hasArmor("CONE"));
        assertTrue(alliedBucket.getArmorSet().hasArmor("BUCKET"));
    }

    @Test
    void catTailMintTriggersMagnetShroomPlantFood() {
        Plant magnet = placePlant("Magnet-shroom", 6, 3);
        Zombie bucket = frozenZombie("ZombieArmor2", 3, 3);

        placePlant("catTail-mint", 8, 3);

        assertTrue(magnet.isPlantFoodActive(game.getCurrentTick()));
        assertFalse(bucket.getArmorSet().hasArmor("BUCKET"));
    }


    @Test
    void levelTwoRangeUpgradeReachesOneAdditionalColumn() {
        Plant levelOne = plantFactory.create("Magnet-shroom", 1);
        world.board().plant(6, 3, levelOne);
        levelOne.place(world, 6, 3, game.getCurrentTick());
        game.register(levelOne);
        Zombie farBucket = frozenZombie("ZombieArmor2", 1, 3);

        game.advance(MAGNET_INTERVAL_TICKS);
        assertTrue(farBucket.getArmorSet().hasArmor("BUCKET"));

        levelOne.tryRemove(pvz.model.entity.plant.lifecycle.PlantThreat.FORCED_REMOVAL);
        Plant levelTwo = plantFactory.create("Magnet-shroom", 2);
        world.board().plant(6, 3, levelTwo);
        levelTwo.place(world, 6, 3, game.getCurrentTick());
        game.register(levelTwo);

        game.advance(MAGNET_INTERVAL_TICKS);
        assertFalse(farBucket.getArmorSet().hasArmor("BUCKET"));
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        world.board().plant(column, row, plant);
        plant.place(world, column, row, game.getCurrentTick());
        game.register(plant);
        return plant;
    }

    private Zombie frozenZombie(String id, int column, int row) {
        Zombie zombie = zombieFactory.create(id, 3);
        zombie.spawn(world, column, row);
        zombie.applyFreeze(game.getCurrentTick(), 4000);
        return zombie;
    }
}
