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
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.behavior.capability.ArmableTrapCapability;
import pvz.model.entity.plant.behavior.capability.PlantArmorCapability;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;
import pvz.model.entity.plant.hit.PlantHitSource;

class ImitaterPlaceholderBehaviorTest {

    private Game game;
    private World world;
    private PlantFactory plantFactory;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        world = new World(
                game,
                new Board(9, 5),
                new BattleResources(1000, 0),
                new Random(101)
        );
        plantFactory = new PlantFactory(
                PlantCsvLoader.load("assets/Data/plants.csv").byName()
        );
        game.register(world.board());
    }

    @Test
    void imitaterKeepsItsOwnIdentityButUsesWallNutPlaceholderStats() {
        Plant imitater = plantFactory.create("Imitater");
        Plant wallNut = plantFactory.create("Wall-nut");

        assertEquals("Imitater", imitater.getName());
        assertEquals(PlantCategory.MODIFIER, imitater.getSpec().getCategory());
        assertEquals("Wall-nut", wallNut.getName());
        assertEquals(PlantCategory.WALL, wallNut.getSpec().getCategory());

        assertEquals(wallNut.getSpec().getCost(), imitater.getSpec().getCost());
        assertEquals(wallNut.getHealth(), imitater.getHealth());
        assertEquals(wallNut.getSpec().getDamage(), imitater.getSpec().getDamage());
        assertEquals(wallNut.getSpec().getRecharge(), imitater.getSpec().getRecharge());
        assertEquals(wallNut.getSpec().getTags(), imitater.getSpec().getTags());

        assertNotNull(armor(imitater));
        assertTrue(PlantFoodSupport.isImplemented(imitater.getSpec()));
        assertFalse(imitater.blocksVaulting());
        assertNull(
                imitater.behaviorCapability(ArmableTrapCapability.class)
        );
    }

    @Test
    void imitaterPlantFoodCreatesTheSamePermanentArmorAsWallNut() {
        Plant imitater = placePlant("Imitater", 5, 3);
        Plant wallNut = placePlant("Wall-nut", 6, 3);

        assertTrue(imitater.tryApplyPlantFood(game.getCurrentTick()));
        assertTrue(wallNut.tryApplyPlantFood(game.getCurrentTick()));

        assertEquals(wallNut.getArmorCapacity(), imitater.getArmorCapacity());
        assertEquals(4000, imitater.getArmorCapacity());
        assertEquals(4000, imitater.getArmorHealth());

        game.advance(2L * Game.TICKS_PER_SECOND + 1);

        assertFalse(imitater.isPlantFoodActive(game.getCurrentTick()));
        assertEquals(4000, imitater.getArmorHealth());
    }

    @Test
    void imitaterArmorAndHealthTakeDamageExactlyLikeWallNut() {
        Plant imitater = placePlant("Imitater", 5, 3);
        Plant wallNut = placePlant("Wall-nut", 6, 3);

        assertTrue(imitater.tryApplyPlantFood(game.getCurrentTick()));
        assertTrue(wallNut.tryApplyPlantFood(game.getCurrentTick()));

        game.advance(2L * Game.TICKS_PER_SECOND + 1);

        imitater.receiveHit(PlantHitSource.PROJECTILE, null, 4500);
        wallNut.receiveHit(PlantHitSource.PROJECTILE, null, 4500);

        assertEquals(wallNut.getArmorHealth(), imitater.getArmorHealth());
        assertEquals(0, imitater.getArmorHealth());
        assertEquals(wallNut.getHealth(), imitater.getHealth());
        assertEquals(3500, imitater.getHealth());
    }

    private PlantArmorCapability armor(Plant plant) {
        return plant.behaviorCapability(PlantArmorCapability.class);
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        world.board().plant(column, row, plant);
        plant.place(world, column, row, game.getCurrentTick());
        game.register(plant);
        return plant;
    }
}
