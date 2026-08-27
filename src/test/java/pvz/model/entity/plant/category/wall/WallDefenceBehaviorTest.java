package pvz.model.entity.plant.category.wall;

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
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.PlantStackingRole;
import pvz.model.entity.plant.hit.PlantHitSource;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class WallDefenceBehaviorTest {

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
                new Random(5)
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
    void tallNutBlocksDodoVaultAndWallNutDoesNot() {
        placePlant("Tall-nut", 4, 1);
        Zombie blockedDodo = spawnZombie("ZombieIceAgeDodo", 4, 1);
        double blockedPosition = blockedDodo.getX();

        placePlant("Wall-nut", 4, 3);
        Zombie vaultingDodo = spawnZombie("ZombieIceAgeDodo", 4, 3);
        double vaultingPosition = vaultingDodo.getX();

        game.advance(1);

        assertEquals(blockedPosition, blockedDodo.getX());
        assertTrue(vaultingDodo.getX() < vaultingPosition - 0.5);
    }

    @Test
    void endurianReflectsOnlyValidBites() {
        Plant endurian = placePlant("Endurian", 4, 2);
        Zombie zombie = spawnZombie("ZombieDefault", 4, 2);
        double healthBefore = zombie.getHealth();

        endurian.receiveHit(PlantHitSource.PROJECTILE, null, 100);
        world.board().damagePlantsInArea(4, 2, 0, 100);

        assertEquals(healthBefore, zombie.getHealth());

        endurian.receiveHit(PlantHitSource.BITE, zombie, 100);

        assertEquals(healthBefore - 20, zombie.getHealth());
    }

    @Test
    void endurianReflectIsDoubledWhileArmorIsIntact() {
        Plant endurian = placePlant("Endurian", 4, 2);
        assertTrue(endurian.tryApplyPlantFood(game.getCurrentTick()));
        game.advance(PLANT_FOOD_TICKS);

        Zombie zombie = spawnZombie("ZombieDefault", 4, 2);
        double healthBefore = zombie.getHealth();

        endurian.receiveHit(PlantHitSource.BITE, zombie, 100);

        assertEquals(healthBefore - 40, zombie.getHealth());
        assertEquals(2900, endurian.getArmorHealth());
        assertEquals(3000, endurian.getHealth());

        endurian.receiveHit(PlantHitSource.PROJECTILE, null, 2900);
        assertFalse(endurian.hasIntactArmor());

        double healthAfterArmorBreak = zombie.getHealth();
        endurian.receiveHit(PlantHitSource.BITE, zombie, 100);

        assertEquals(healthAfterArmorBreak - 20, zombie.getHealth());
    }

    @Test
    void pumpkinShieldsThePlantUnderneathUntilItIsDestroyed() {
        Plant peashooter = placePlant("Peashooter", 6, 2);
        Plant pumpkin = placePlant("Pumpkin", 6, 2);

        assertEquals(
                PlantStackingRole.PROTECTIVE_COVER,
                pumpkin.getStackingRole()
        );
        assertEquals(
                pumpkin,
                world.board().getTopPlant(6, 2)
        );

        double peashooterHealth = peashooter.getHealth();

        world.board().damagePlantsInArea(6, 2, 0, 500);

        assertEquals(3500, pumpkin.getHealth());
        assertEquals(peashooterHealth, peashooter.getHealth());

        Plant secondPumpkin = plantFactory.create("Pumpkin");
        assertFalse(
                world.board().plant(6, 2, secondPumpkin)
                        .startsWith("planted ")
        );

        world.board().damagePlantsInArea(6, 2, 0, 3500);

        assertTrue(pumpkin.isRemovedFromWorld());
        assertFalse(peashooter.isRemovedFromWorld());
        assertEquals(peashooterHealth, peashooter.getHealth());
        assertEquals(peashooter, world.board().getTopPlant(6, 2));

        world.board().damagePlantsInArea(6, 2, 0, 100);

        assertEquals(peashooterHealth - 100, peashooter.getHealth());
    }

    @Test
    void zombieBiteTargetsThePumpkinBeforeTheProtectedPlant() {
        Plant peashooter = placePlant("Peashooter", 6, 4);
        Plant pumpkin = placePlant("Pumpkin", 6, 4);
        spawnZombie("ZombieDefault", 6, 4);

        double peashooterHealth = peashooter.getHealth();

        game.advance(Game.TICKS_PER_SECOND + 1);

        assertTrue(pumpkin.getHealth() < 4000);
        assertEquals(peashooterHealth, peashooter.getHealth());
    }

    @Test
    void explodeONutExplodesOnceForArmorAndOnceForTheBody() {
        Plant explodeONut = placePlant("Explode-o-nut", 5, 3);
        assertTrue(explodeONut.tryApplyPlantFood(game.getCurrentTick()));
        game.advance(PLANT_FOOD_TICKS);

        Zombie bucketHead = spawnZombie("ZombieArmor2", 5, 3);
        GameEvents.drain();

        explodeONut.receiveHit(PlantHitSource.PROJECTILE, null, 100000);

        List<String> events = GameEvents.drain();

        assertEquals(1, countEvents(events, "armor exploded."));
        assertEquals(1, countEvents(events, ") exploded."));
        assertTrue(explodeONut.isRemovedFromWorld());
        assertTrue(bucketHead.isDead());
    }

    @Test
    void pluckAndCleanupNeverExplodeExplodeONut() {
        Plant explodeONut = placePlant("Explode-o-nut", 5, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 5, 3);
        double zombieHealth = zombie.getHealth();
        GameEvents.drain();

        assertEquals(
                pvz.model.entity.plant.lifecycle.PlantRemovalResult.REMOVED,
                explodeONut.tryRemove(PlantThreat.PLUCK)
        );

        assertEquals(zombieHealth, zombie.getHealth());
        assertEquals(0, countEvents(GameEvents.drain(), "exploded"));

        Plant cleanedUp = placePlant("Explode-o-nut", 6, 3);
        cleanedUp.tryRemove(PlantThreat.SYSTEM_CLEANUP);

        assertEquals(zombieHealth, zombie.getHealth());
        assertEquals(0, countEvents(GameEvents.drain(), "exploded"));
    }

    private int countEvents(List<String> events, String fragment) {
        return (int) events.stream()
                .filter(event -> event.contains(fragment))
                .count();
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
