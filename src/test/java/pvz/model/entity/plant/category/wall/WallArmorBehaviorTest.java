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
import pvz.data.PlantData;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.hit.PlantHitSource;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;

class WallArmorBehaviorTest {

    private static final long PLANT_FOOD_TICKS =
            2L * Game.TICKS_PER_SECOND + 1;

    private Game game;
    private World world;
    private PlantFactory plantFactory;
    private PlantData plantData;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        world = new World(
                game,
                new Board(9, 5),
                new BattleResources(500, 0),
                new Random(7)
        );
        plantData = PlantCsvLoader.load("assets/Data/plants.csv");
        plantFactory = new PlantFactory(plantData.byName());
        game.register(world.board());
        GameEvents.drain();
    }

    @Test
    void plantFoodArmorCapacityEqualsBaseHealthOfEachWall() {
        assertEquals(4000, armoredWall("Wall-nut", 1).getArmorCapacity());
        assertEquals(8000, armoredWall("Tall-nut", 2).getArmorCapacity());
        assertEquals(3000, armoredWall("Endurian", 3).getArmorCapacity());
        assertEquals(
                4000,
                armoredWall("Explode-o-nut", 4).getArmorCapacity()
        );
        assertEquals(4000, armoredWall("Pumpkin", 5).getArmorCapacity());
        assertEquals(1000, armoredWall("Sun Bean", 6).getArmorCapacity());
    }

    @Test
    void armorTakesDamageBeforeHealthAndOverflowsInTheSameHit() {
        Plant wallNut = armoredWall("Wall-nut", 5);

        wallNut.receiveHit(PlantHitSource.PROJECTILE, null, 1000);

        assertEquals(3000, wallNut.getArmorHealth());
        assertEquals(4000, wallNut.getHealth());

        wallNut.receiveHit(PlantHitSource.PROJECTILE, null, 3500);

        assertEquals(0, wallNut.getArmorHealth());
        assertEquals(3500, wallNut.getHealth());
        assertFalse(wallNut.isRemovedFromWorld());
    }

    @Test
    void areaDamageAlsoReachesTheWallArmorFirst() {
        Plant tallNut = armoredWall("Tall-nut", 5);

        world.board().damagePlantsInArea(5, 3, 0, 2000);

        assertEquals(6000, tallNut.getArmorHealth());
        assertEquals(8000, tallNut.getHealth());
    }

    @Test
    void armorRemainsAfterPlantFoodEndsAndCanBeRefilledAgain() {
        Plant wallNut = armoredWall("Wall-nut", 5);

        wallNut.receiveHit(PlantHitSource.PROJECTILE, null, 3000);
        assertEquals(1000, wallNut.getArmorHealth());
        assertEquals(4000, wallNut.getHealth());

        assertTrue(wallNut.tryApplyPlantFood(game.getCurrentTick()));
        assertEquals(4000, wallNut.getArmorHealth());

        game.advance(PLANT_FOOD_TICKS);

        assertFalse(wallNut.isPlantFoodActive(game.getCurrentTick()));
        assertEquals(4000, wallNut.getArmorHealth());
    }

    @Test
    void wallsWithoutPlantFoodEffectStayUnarmored() {
        assertFalse(
                PlantFoodSupport.isImplemented(
                        plantData.byName().get("reinforce-mint")
                )
        );

        Plant reinforceMint = plantFactory.create("Reinforce-mint");
        assertFalse(reinforceMint.supportsPlantFood());

        for (String name : List.of(
                "wall-nut",
                "tall-nut",
                "endurian",
                "garlic",
                "sweet potato",
                "explode-o-nut",
                "pumpkin",
                "sun bean"
        )) {
            assertTrue(
                    PlantFoodSupport.isImplemented(
                            plantData.byName().get(name)
                    ),
                    name + " must support plant food"
            );
        }
    }

    @Test
    void garlicAndSweetPotatoPlantFoodDoNotCreateArmor() {
        Plant garlic = placePlant("Garlic", 2, 2);
        Plant sweetPotato = placePlant("Sweet Potato", 3, 4);

        assertTrue(garlic.tryApplyPlantFood(game.getCurrentTick()));
        assertTrue(sweetPotato.tryApplyPlantFood(game.getCurrentTick()));

        assertEquals(0, garlic.getArmorCapacity());
        assertEquals(0, garlic.getArmorHealth());
        assertEquals(0, sweetPotato.getArmorCapacity());
        assertEquals(0, sweetPotato.getArmorHealth());
    }

    private Plant armoredWall(String name, int column) {
        Plant plant = placePlant(name, column, 3);
        assertTrue(plant.tryApplyPlantFood(game.getCurrentTick()));
        assertEquals(plant.getArmorCapacity(), plant.getArmorHealth());

        game.advance(PLANT_FOOD_TICKS);

        return plant;
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        world.board().plant(column, row, plant);
        plant.place(world, column, row, game.getCurrentTick());
        return plant;
    }
}
