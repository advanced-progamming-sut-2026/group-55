package pvz.model.entity.plant.category.strikethrough;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pvz.data.PlantCsvLoader;
import pvz.data.ZombieCsvLoader;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieAllegiance;
import pvz.model.entity.zombie.ZombieFactory;

class StrikeThroughPlantFoodTest {

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
                new BattleResources(5000, 20),
                new Random(271)
        );
        plantFactory = new PlantFactory(
                PlantCsvLoader.load("assets/Data/plants.csv").byName()
        );
        zombieFactory = new ZombieFactory(
                ZombieCsvLoader.load("assets/Data/zombies.csv")
        );
        game.register(world.board());
    }

    @Test
    void bothStrikeThroughPlantsSupportPlantFood() {
        for (String name : List.of("Cactus", "Fume-shroom")) {
            Plant plant = plantFactory.create(name);
            assertTrue(PlantFoodSupport.isImplemented(plant.getSpec()), name);
            assertTrue(plant.supportsPlantFood(), name);
        }
    }

    @Test
    void cactusPlantFoodPermanentlyElectrifiesAndUnlocksUnlimitedPiercing() {
        Plant cactus = placePlant("Cactus", 2, 3);
        StrikeThroughStateCapability state = cactus.behaviorCapability(
                StrikeThroughStateCapability.class
        );
        assertNotNull(state);
        assertEquals(CactusStage.NORMAL, state.getCactusStage());

        assertTrue(cactus.tryApplyPlantFood(game.getCurrentTick()));
        assertEquals(CactusStage.ELECTRIFIED, state.getCactusStage());
        assertFalse(cactus.canApplyPlantFood(game.getCurrentTick()));

        game.advance(20);
        assertFalse(cactus.canApplyPlantFood(game.getCurrentTick()));

        List<Zombie> zombies = spawnFrozenGargantuars(4, 4, 3);
        List<Double> healthBefore = healthSnapshot(zombies);
        game.advance(25);

        assertEquals(4, countDamaged(zombies, healthBefore, 60));
        assertEquals(CactusStage.ELECTRIFIED, state.getCactusStage());
    }

    @Test
    void fumeShroomPlantFoodDamagesAndPushesHostilesAheadAcrossItsLane() {
        Plant fume = placePlant("Fume-shroom", 3, 3);
        Zombie near = spawnZombie("ZombieGargantuar", 4, 3);
        Zombie far = spawnZombie("ZombieGargantuar", 7, 3);
        Zombie behind = spawnZombie("ZombieGargantuar", 2, 3);
        Zombie otherRow = spawnZombie("ZombieGargantuar", 5, 2);
        Zombie allied = spawnZombie("ZombieGargantuar", 5, 3);
        world.changeZombieAllegiance(allied, ZombieAllegiance.ALLIED);

        double nearHealth = near.getHealth();
        double farHealth = far.getHealth();
        double behindHealth = behind.getHealth();
        double otherRowHealth = otherRow.getHealth();
        double alliedHealth = allied.getHealth();

        assertTrue(fume.tryApplyPlantFood(game.getCurrentTick()));

        assertEquals(nearHealth - 1500, near.getHealth());
        assertEquals(farHealth - 1500, far.getHealth());
        assertEquals(8.5, near.getX());
        assertEquals(8.5, far.getX());
        assertEquals(behindHealth, behind.getHealth());
        assertEquals(otherRowHealth, otherRow.getHealth());
        assertEquals(alliedHealth, allied.getHealth());
    }

    @Test
    void pierceMintNowActivatesBothImplementedFamilyPlantFoods() {
        Plant cactus = placePlant("Cactus", 2, 2);
        Plant fume = placePlant("Fume-shroom", 2, 3);
        Zombie target = spawnZombie("ZombieGargantuar", 5, 3);
        double healthBefore = target.getHealth();

        placePlant("Pierce-mint", 6, 5);

        StrikeThroughStateCapability cactusState = cactus.behaviorCapability(
                StrikeThroughStateCapability.class
        );
        assertNotNull(cactusState);
        assertEquals(CactusStage.ELECTRIFIED, cactusState.getCactusStage());
        assertTrue(fume.isPlantFoodActive(game.getCurrentTick()));
        assertEquals(healthBefore - 1500, target.getHealth());
        assertEquals(8.5, target.getX());
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        assertNotNull(plant);
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

    private List<Zombie> spawnFrozenGargantuars(
            int count,
            int column,
            int row
    ) {
        List<Zombie> zombies = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Zombie zombie = spawnZombie("ZombieGargantuar", column, row);
            zombie.applyFreeze(game.getCurrentTick(), 100);
            zombies.add(zombie);
        }
        return zombies;
    }

    private List<Double> healthSnapshot(List<Zombie> zombies) {
        return zombies.stream().map(Zombie::getHealth).toList();
    }

    private long countDamaged(
            List<Zombie> zombies,
            List<Double> healthBefore,
            double expectedDamage
    ) {
        long count = 0;
        for (int index = 0; index < zombies.size(); index++) {
            double damage = healthBefore.get(index) - zombies.get(index).getHealth();
            if (damage == expectedDamage) {
                count++;
            }
        }
        return count;
    }
}
