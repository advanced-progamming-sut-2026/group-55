package pvz.model.entity.plant.category.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pvz.data.PlantCsvLoader;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;
import pvz.model.entity.zombie.PushedObstacle;

class LilyPadBehaviorTest {

    private Game game;
    private World world;
    private PlantFactory plantFactory;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        world = new World(
                game,
                new Board(7, 5),
                new BattleResources(1000, 0),
                new Random(83)
        );
        plantFactory = new PlantFactory(
                PlantCsvLoader.load("assets/Data/plants.csv").byName()
        );
        world.setPlantCreator(plantFactory::create);
        game.register(world.board());
    }

    @Test
    void lilyPadProvidesTheExistingWaterPlatformStackingBehavior() {
        setWater(4, 3);

        Plant lilyPad = placePlant("Lily Pad", 4, 3);
        Plant peashooter = placePlant("Peashooter", 4, 3);

        assertFalse(lilyPad.isRemovedFromWorld());
        assertFalse(peashooter.isRemovedFromWorld());
        assertEquals(2, world.board().getTile(4, 3).getPlants().size());
        assertEquals(lilyPad, world.board().getTile(4, 3).getPlants().getFirst());
        assertEquals(peashooter, world.board().getTopPlant(4, 3));
        assertTrue(PlantFoodSupport.isImplemented(lilyPad.getSpec()));
    }

    @Test
    void plantFoodCreatesFourOrthogonalLilyPadsOnEmptyWater() {
        setWater(4, 3);
        setWater(3, 3);
        setWater(5, 3);
        setWater(4, 2);
        setWater(4, 4);
        setWater(3, 2);

        Plant lilyPad = placePlant("Lily Pad", 4, 3);
        int sunBefore = world.sunBank().getBalance();

        assertTrue(lilyPad.tryApplyPlantFood(game.getCurrentTick()));

        assertLilyAt(3, 3);
        assertLilyAt(5, 3);
        assertLilyAt(4, 2);
        assertLilyAt(4, 4);
        assertTrue(world.board().getTile(3, 2).getPlants().isEmpty());
        assertEquals(5, world.getPlants().size());
        assertEquals(sunBefore, world.sunBank().getBalance());
        assertEquals(6, game.getRegisteredObjectCount());
    }

    @Test
    void abilitySpawnedClonesDoNotReceivePlantFoodOrRecurse() {
        setWaterCross(4, 3);
        Plant lilyPad = placePlant("Lily Pad", 4, 3);

        assertTrue(lilyPad.tryApplyPlantFood(game.getCurrentTick()));

        for (Plant plant : world.getPlants()) {
            if (plant == lilyPad) {
                assertTrue(plant.isPlantFoodActive(game.getCurrentTick()));
            } else {
                assertFalse(plant.isPlantFoodActive(game.getCurrentTick()));
                assertTrue(plant.canApplyPlantFood(game.getCurrentTick()));
            }
        }

        game.advance(1);
        assertEquals(5, world.getPlants().size());
    }

    @Test
    void plantFoodSkipsOccupiedCraterAndGroundObstacleTiles() {
        setWaterCross(4, 3);
        placePlant("Lily Pad", 3, 3);
        world.board().placeCrater(5, 3, game.getCurrentTick(), 100);

        PushedObstacle obstacle = new PushedObstacle(
                "test-block",
                "Test Block",
                100,
                true,
                true,
                false
        );
        obstacle.spawn(world, 3.5, 1.5);

        Plant lilyPad = placePlant("Lily Pad", 4, 3);
        assertTrue(lilyPad.tryApplyPlantFood(game.getCurrentTick()));

        assertEquals(1, countPlantsAt(3, 3));
        assertEquals(0, countPlantsAt(5, 3));
        assertEquals(0, countPlantsAt(4, 2));
        assertLilyAt(4, 4);
    }

    @Test
    void plantFoodAtBoardEdgeNeverCreatesOutOfBoundsPlants() {
        setWater(1, 1);
        setWater(2, 1);
        setWater(1, 2);

        Plant lilyPad = placePlant("Lily Pad", 1, 1);
        assertTrue(lilyPad.tryApplyPlantFood(game.getCurrentTick()));

        assertLilyAt(2, 1);
        assertLilyAt(1, 2);
        assertEquals(3, world.getPlants().size());
    }

    @Test
    void removingWaterPlatformSinksUnsupportedPlantsEvenDuringPlantFood() {
        setWater(4, 3);
        Plant lilyPad = placePlant("Lily Pad", 4, 3);
        Plant peashooter = placePlant("Peashooter", 4, 3);

        assertTrue(peashooter.tryApplyPlantFood(game.getCurrentTick()));
        assertTrue(peashooter.isPlantFoodActive(game.getCurrentTick()));

        assertEquals(
                pvz.model.entity.plant.lifecycle.PlantRemovalResult.REMOVED,
                lilyPad.tryRemove(PlantThreat.PLUCK)
        );

        assertTrue(lilyPad.isRemovedFromWorld());
        assertTrue(peashooter.isRemovedFromWorld());
        assertTrue(world.board().getTile(4, 3).getPlants().isEmpty());
    }

    @Test
    void removingWaterPlatformDoesNotSinkWaterNativePlants() {
        setWater(4, 3);
        Plant lilyPad = placePlant("Lily Pad", 4, 3);
        Plant tangleKelp = placePlant("Tangle Kelp", 4, 3);

        lilyPad.tryRemove(PlantThreat.PLUCK);

        assertTrue(lilyPad.isRemovedFromWorld());
        assertFalse(tangleKelp.isRemovedFromWorld());
        assertEquals(tangleKelp, world.board().getTopPlant(4, 3));
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        assertNotNull(plant);

        world.board().plant(column, row, plant);
        assertTrue(
                world.board().getTile(column, row).getPlants().contains(plant),
                () -> name + " was not placed at (" + column + ", " + row + ")"
        );

        plant.place(world, column, row, game.getCurrentTick());
        if (!plant.isRemovedFromWorld()) {
            game.register(plant);
        }
        return plant;
    }

    private void setWaterCross(int column, int row) {
        setWater(column, row);
        setWater(column - 1, row);
        setWater(column + 1, row);
        setWater(column, row - 1);
        setWater(column, row + 1);
    }

    private void setWater(int column, int row) {
        world.board().setTileType(column, row, TileType.WATER);
    }

    private void assertLilyAt(int column, int row) {
        Plant plant = world.board().getTopPlant(column, row);
        assertNotNull(plant);
        assertEquals("Lily Pad", plant.getName());
    }

    private int countPlantsAt(int column, int row) {
        return world.board().getTile(column, row).getPlants().size();
    }
}
