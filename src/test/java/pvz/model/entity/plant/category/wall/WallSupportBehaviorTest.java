package pvz.model.entity.plant.category.wall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.collectible.sun.SunSource;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.hit.PlantHitSource;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class WallSupportBehaviorTest {

    private static final long PLANT_FOOD_TICKS =
            2L * Game.TICKS_PER_SECOND + 1;

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
                new BattleResources(500, 0),
                new Random(11)
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
    void garlicPushesTheBitingZombieIntoAnAdjacentRow() {
        Plant garlic = placePlant("Garlic", 4, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 4, 3);
        double healthBefore = zombie.getHealth();

        assertTrue(garlic.receiveHit(PlantHitSource.BITE, zombie, 100));

        assertTrue(List.of(2, 4).contains(zombie.getRow()));
        assertEquals(healthBefore, zombie.getHealth());
        assertEquals(200, garlic.getHealth());
        assertFalse(zombie.isEating());
    }

    @Test
    void garlicKeepsBorderRowZombiesInsideTheBoard() {
        Plant topGarlic = placePlant("Garlic", 4, 1);
        Zombie topZombie = spawnZombie("ZombieDefault", 4, 1);
        topGarlic.receiveHit(PlantHitSource.BITE, topZombie, 100);
        assertEquals(2, topZombie.getRow());

        Plant bottomGarlic = placePlant("Garlic", 4, 5);
        Zombie bottomZombie = spawnZombie("ZombieDefault", 4, 5);
        bottomGarlic.receiveHit(PlantHitSource.BITE, bottomZombie, 100);
        assertEquals(4, bottomZombie.getRow());
    }

    @Test
    void garlicPlantFoodMovesEveryZombieOfItsRowOnce() {
        Plant garlic = placePlant("Garlic", 4, 3);
        List<Zombie> zombies = List.of(
                spawnZombie("ZombieDefault", 9, 3),
                spawnZombie("ZombieDefault", 7, 3),
                spawnZombie("ZombieDefault", 6, 3)
        );
        Zombie otherRow = spawnZombie("ZombieDefault", 8, 5);

        assertTrue(garlic.tryApplyPlantFood(game.getCurrentTick()));

        for (Zombie zombie : zombies) {
            assertNotEquals(3, zombie.getRow());
            assertTrue(List.of(2, 4).contains(zombie.getRow()));
        }

        assertEquals(5, otherRow.getRow());
    }

    @Test
    void sweetPotatoOnlyAttractsAdjacentRowZombiesInRange() {
        placePlant("Sweet Potato", 5, 3);
        Zombie inRange = spawnZombie("ZombieDefault", 5, 2);
        Zombie outOfRange = spawnZombie("ZombieDefault", 9, 2);
        Zombie twoRowsAway = spawnZombie("ZombieDefault", 5, 1);
        Zombie sameRow = spawnZombie("ZombieDefault", 5, 3);

        game.advance(1);

        assertEquals(3, inRange.getRow());
        assertEquals(2, outOfRange.getRow());
        assertEquals(1, twoRowsAway.getRow());
        assertEquals(3, sameRow.getRow());
    }

    @Test
    void sweetPotatoPlantFoodPullsRangeZombiesAndRestoresHealth() {
        Plant sweetPotato = placePlant("Sweet Potato", 5, 3);
        sweetPotato.receiveHit(PlantHitSource.PROJECTILE, null, 500);
        assertEquals(2500, sweetPotato.getHealth());

        Zombie zombie = spawnZombie("ZombieDefault", 5, 4);

        assertTrue(sweetPotato.tryApplyPlantFood(game.getCurrentTick()));

        assertEquals(3000, sweetPotato.getHealth());
        assertEquals(3, zombie.getRow());
        assertEquals(0, sweetPotato.getArmorHealth());
    }

    @Test
    void sunBeanProducesOneHundredValueSunPerTwoHundredFiftyDamage() {
        Plant sunBean = placePlant("Sun Bean", 4, 2);
        Zombie zombie = spawnZombie("ZombieDefault", 4, 2);
        int bankBefore = world.sunBank().getBalance();

        sunBean.receiveHit(PlantHitSource.BITE, zombie, 100);

        assertTrue(producedSuns().isEmpty());
        assertEquals(bankBefore, world.sunBank().getBalance());

        sunBean.receiveHit(PlantHitSource.PROJECTILE, null, 150);
        assertEquals(1, producedSuns().size());
        assertEquals(100, producedSuns().getFirst().getValue());

        sunBean.receiveHit(PlantHitSource.AREA_DAMAGE, null, 500);
        assertEquals(3, producedSuns().size());

        assertTrue(
                producedSuns().stream()
                        .allMatch(sun -> sun.getValue() == 100)
        );
    }

    @Test
    void sunBeanProducesSunEvenWhenTheArmorAbsorbsTheHit() {
        Plant sunBean = placePlant("Sun Bean", 4, 2);
        assertTrue(sunBean.tryApplyPlantFood(game.getCurrentTick()));
        game.advance(PLANT_FOOD_TICKS);

        sunBean.receiveHit(PlantHitSource.PROJECTILE, null, 250);

        assertEquals(750, sunBean.getArmorHealth());
        assertEquals(1000, sunBean.getHealth());
        assertEquals(1, producedSuns().size());
        assertEquals(100, producedSuns().getFirst().getValue());
    }

    @Test
    void sunBeanAccumulatesSmallAppliedHitsUntilTheThreshold() {
        Plant sunBean = placePlant("Sun Bean", 4, 2);

        for (int hit = 0; hit < 49; hit++) {
            sunBean.receiveHit(
                    PlantHitSource.PROJECTILE,
                    null,
                    5
            );
        }

        assertTrue(producedSuns().isEmpty());

        sunBean.receiveHit(PlantHitSource.PROJECTILE, null, 5);

        assertEquals(1, producedSuns().size());
        assertEquals(100, producedSuns().getFirst().getValue());
    }

    @Test
    void sunBeanDropsUseTheSharedCollectibleAndRaStealPath() {
        Plant sunBean = placePlant("Sun Bean", 4, 2);
        sunBean.receiveHit(PlantHitSource.PROJECTILE, null, 250);
        Sun droppedSun = producedSuns().getFirst();
        int bankBefore = world.sunBank().getBalance();

        Zombie ra = spawnZombie("ZombieRa", 9, 1);
        ra.update(game.getCurrentTick());

        assertTrue(droppedSun.isRemoved());
        assertTrue(producedSuns().isEmpty());
        assertEquals(bankBefore, world.sunBank().getBalance());

        ra.takeDirectDamage(Double.MAX_VALUE);

        assertEquals(bankBefore + 100, world.sunBank().getBalance());
    }

    @Test
    void sunBeanIgnoresOverkillDamageBeyondItsRemainingDurability() {
        Plant sunBean = placePlant("Sun Bean", 4, 2);

        sunBean.receiveHit(
                PlantHitSource.PROJECTILE,
                null,
                100_000
        );

        assertTrue(sunBean.isRemovedFromWorld());
        assertEquals(4, producedSuns().size());
        assertTrue(
                producedSuns().stream()
                        .allMatch(sun -> sun.getValue() == 100)
        );
    }

    @Test
    void zeroDamagePluckAndCleanupNeverProduceSun() {
        Plant sunBean = placePlant("Sun Bean", 4, 2);

        assertFalse(sunBean.receiveHit(PlantHitSource.PROJECTILE, null, 0));
        sunBean.tryRemove(
                pvz.model.entity.plant.lifecycle.PlantThreat.PLUCK
        );

        assertTrue(producedSuns().isEmpty());

        Plant cleaned = placePlant("Sun Bean", 5, 2);
        cleaned.tryRemove(
                pvz.model.entity.plant.lifecycle.PlantThreat.SYSTEM_CLEANUP
        );

        assertTrue(producedSuns().isEmpty());
    }

    @Test
    void hitsBlockedByPlantFoodCreateNoReactionAtAll() {
        Plant sunBean = placePlant("Sun Bean", 4, 2);
        Plant endurian = placePlant("Endurian", 6, 2);
        Plant garlic = placePlant("Garlic", 8, 2);

        assertTrue(sunBean.tryApplyPlantFood(game.getCurrentTick()));
        assertTrue(endurian.tryApplyPlantFood(game.getCurrentTick()));
        assertTrue(garlic.tryApplyPlantFood(game.getCurrentTick()));

        Zombie zombie = spawnZombie("ZombieDefault", 4, 4);
        double zombieHealth = zombie.getHealth();
        int zombieRow = zombie.getRow();

        assertFalse(sunBean.receiveHit(PlantHitSource.BITE, zombie, 100));
        assertFalse(endurian.receiveHit(PlantHitSource.BITE, zombie, 100));
        assertFalse(garlic.receiveHit(PlantHitSource.BITE, zombie, 100));

        assertEquals(1000, sunBean.getHealth());
        assertEquals(1000, sunBean.getArmorHealth());
        assertEquals(3000, endurian.getHealth());
        assertEquals(3000, endurian.getArmorHealth());
        assertEquals(300, garlic.getHealth());
        assertEquals(zombieHealth, zombie.getHealth());
        assertEquals(zombieRow, zombie.getRow());
        assertTrue(producedSuns().isEmpty());
    }

    @Test
    void realZombieBitesReachWallReactionsThroughTheSharedPath() {
        Plant sunBean = placePlant("Sun Bean", 4, 2);
        spawnZombie("ZombieDefault", 4, 2);

        game.advance(Game.TICKS_PER_SECOND + 1);

        assertEquals(900, sunBean.getHealth());
        assertTrue(producedSuns().isEmpty());

        game.advance(2L * Game.TICKS_PER_SECOND);

        assertEquals(700, sunBean.getHealth());
        assertEquals(1, producedSuns().size());
        assertEquals(100, producedSuns().getFirst().getValue());

        Plant garlic = placePlant("Garlic", 6, 4);
        Zombie repelled = spawnZombie("ZombieDefault", 6, 4);

        game.advance(Game.TICKS_PER_SECOND + 1);

        assertEquals(200, garlic.getHealth());
        assertTrue(List.of(3, 5).contains(repelled.getRow()));
    }

    private List<Sun> producedSuns() {
        return world.getCollectibles().stream()
                .filter(Sun.class::isInstance)
                .map(Sun.class::cast)
                .filter(sun -> sun.getSource() == SunSource.PLANT)
                .toList();
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
}
