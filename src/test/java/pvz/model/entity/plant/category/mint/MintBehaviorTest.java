package pvz.model.entity.plant.category.mint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pvz.data.PlantCsvLoader;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.PlantTag;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;
import pvz.model.entity.plant.category.modifier.TorchwoodStage;
import pvz.model.entity.plant.category.modifier.TorchwoodStateCapability;

class MintBehaviorTest {

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
                new Random(151)
        );
        plantFactory = new PlantFactory(
                PlantCsvLoader.load("assets/Data/plants.csv").byName()
        );
        game.register(world.board());
    }

    @Test
    void allMintRowsUseTheSharedMintBranchAndDoNotAcceptPlantFood() {
        List<String> names = List.of(
                "Enlighten-mint",
                "Appease-mint",
                "Arma-mint",
                "Bombard-mint",
                "Enforce-mint",
                "Reinforce-mint",
                "Enchant-mint",
                "Pierce-mint",
                "catTail-mint"
        );

        for (String name : names) {
            Plant mint = plantFactory.create(name);
            assertNotNull(mint);
            assertTrue(mint.getSpec().getTags().contains(PlantTag.MINT));
            assertTrue(MintBehaviorFactory.isMint(mint.getSpec()));
            assertFalse(PlantFoodSupport.isImplemented(mint.getSpec()));
        }
    }

    @Test
    void appeaseMintFeedsOnlyShooterFamilyPlantsAlreadyOnBoard() {
        Plant peashooter = placePlant("Peashooter", 2, 2);
        Plant cabbage = placePlant("Cabbage-pult", 2, 4);

        placePlant("Appease-mint", 5, 3);

        assertTrue(peashooter.isPlantFoodActive(game.getCurrentTick()));
        assertFalse(cabbage.isPlantFoodActive(game.getCurrentTick()));
    }

    @Test
    void enchantMintUsesModifierCategoryAndCanTriggerTorchwoodPlantFood() {
        Plant torchwood = placePlant("Torchwood", 3, 3);
        TorchwoodStateCapability state = torchwood.behaviorCapability(
                TorchwoodStateCapability.class
        );

        assertNotNull(state);
        assertEquals(TorchwoodStage.NORMAL, state.getStage());

        placePlant("Enchant-mint", 6, 3);

        assertEquals(TorchwoodStage.BLUE_FLAME, state.getStage());
    }

    @Test
    void mintDoesNotFeedAnotherMintInTheSameFamily() {
        Plant first = placePlant("Appease-mint", 4, 2);
        Plant second = placePlant("Appease-mint", 5, 2);

        assertFalse(first.supportsPlantFood());
        assertFalse(second.supportsPlantFood());
    }

    @Test
    void mintDisappearsAfterShortDisplayWindow() {
        Plant mint = placePlant("Appease-mint", 5, 3);

        assertTrue(world.getPlants().contains(mint));

        game.advance(4);
        assertTrue(world.getPlants().contains(mint));

        game.advance(1);
        assertFalse(world.getPlants().contains(mint));
    }

    @Test
    void mintHasNormalPlaceholderHealth() {
        Plant mint = placePlant("Appease-mint", 5, 3);

        assertEquals(300.0, mint.getHealth());
        assertFalse(mint.isDead());
    }

    @Test
    void mintDisplayWindowExpiresEvenWhileActionBlocked() {
        Plant mint = placePlant("Appease-mint", 5, 3);
        Object blocker = new Object();

        assertTrue(mint.addActionBlocker(blocker));
        game.advance(5);

        assertFalse(world.getPlants().contains(mint));
    }

    @Test
    void mintDisplayWindowExpiresEvenWhileCovered() {
        Plant mint = placePlant("Appease-mint", 5, 3);

        assertTrue(world.board().coverPlantWithOctopus(mint));
        game.advance(5);

        assertFalse(world.getPlants().contains(mint));
    }

    @Test
    void plantsPlacedAfterMintActivationAreNotRetroactivelyFed() {
        placePlant("Appease-mint", 5, 3);
        Plant peashooter = placePlant("Peashooter", 2, 2);

        assertFalse(peashooter.isPlantFoodActive(game.getCurrentTick()));
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        assertNotNull(plant);
        world.board().plant(column, row, plant);
        plant.place(world, column, row, game.getCurrentTick());
        game.register(plant);
        return plant;
    }
}
