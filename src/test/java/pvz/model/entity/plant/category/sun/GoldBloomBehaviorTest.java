package pvz.model.entity.plant.category.sun;

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
import pvz.model.entity.collectible.Collectible;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.collectible.sun.SunSource;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;

class GoldBloomBehaviorTest {

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
                new Random(1)
        );
        plantData = PlantCsvLoader.load("assets/Data/plants.csv");
        plantFactory = new PlantFactory(plantData.byName());
        game.register(world.board());
        GameEvents.drain();
    }

    @Test
    void goldBloomBurstsFiveSunsOfSeventyFiveOnPlacement() {
        placePlant("Gold Bloom", 3, 2);

        List<Sun> suns = producedSuns();

        assertEquals(5, suns.size());
        assertTrue(suns.stream().allMatch(sun -> sun.getValue() == 75));
        assertEquals(
                375,
                suns.stream().mapToInt(Sun::getValue).sum()
        );
    }

    @Test
    void goldBloomStaysVisibleForFiveTicksAndNeverProducesAgain() {
        Plant goldBloom = placePlant("Gold Bloom", 3, 2);
        game.register(goldBloom);

        assertFalse(goldBloom.isRemovedFromWorld());
        assertEquals(5, producedSuns().size());

        game.advance(4);

        assertFalse(goldBloom.isRemovedFromWorld());
        assertTrue(
                world.board().getTile(3, 2)
                        .getPlants()
                        .contains(goldBloom)
        );
        assertFalse(goldBloom.canBeEatenByZombie());

        game.advance(1);

        assertTrue(goldBloom.isRemovedFromWorld());
        assertFalse(world.getPlants().contains(goldBloom));
        assertFalse(
                world.board().getTile(3, 2)
                        .getPlants()
                        .contains(goldBloom)
        );

        game.advance(2L * Game.TICKS_PER_SECOND);

        assertEquals(5, producedSuns().size());
    }

    @Test
    void goldBloomEffectWindowFinishesWhileActionsAreBlocked() {
        Plant goldBloom = placePlant("Gold Bloom", 3, 2);
        goldBloom.addActionBlocker(new Object());
        game.register(goldBloom);

        game.advance(4);
        assertFalse(goldBloom.isRemovedFromWorld());

        game.advance(1);
        assertTrue(goldBloom.isRemovedFromWorld());
    }

    @Test
    void goldBloomSunsStayCollectableAndExpirableAfterRemoval() {
        placePlant("Gold Bloom", 3, 2);

        int balanceBefore = world.sunBank().getBalance();
        Sun collected = producedSuns().getFirst();
        world.collectSun(collected);

        assertEquals(balanceBefore + 75, world.sunBank().getBalance());
        assertEquals(4, producedSuns().size());

        game.advance(9L * Game.TICKS_PER_SECOND);

        assertTrue(producedSuns().isEmpty());
    }

    @Test
    void goldBloomDoesNotSupportPlantFood() {
        assertFalse(
                PlantFoodSupport.isImplemented(
                        plantData.byName().get("gold bloom")
                )
        );
        assertFalse(plantFactory.create("Gold Bloom").supportsPlantFood());
        assertEquals(
                SunProductionMode.SINGLE_USE_ON_PLACEMENT,
                new GoldBloomProfile().getProductionMode()
        );
    }

    @Test
    void periodicSunProducersKeepTheirPreviousBehavior() {
        Plant sunflower = placePlant("Sunflower", 1, 1);
        game.register(sunflower);
        Plant sunShroom = placePlant("Sun-shroom", 2, 3);
        game.register(sunShroom);

        game.advance(24L * Game.TICKS_PER_SECOND);

        assertFalse(sunflower.isRemovedFromWorld());
        assertFalse(sunShroom.isRemovedFromWorld());
        assertEquals(
                List.of(25, 50),
                producedSuns().stream()
                        .map(Sun::getValue)
                        .sorted()
                        .toList()
        );

        for (String name : List.of(
                "sunflower",
                "twin sunflower",
                "primal sunflower",
                "sun-shroom"
        )) {
            assertTrue(
                    PlantFoodSupport.isImplemented(
                            plantData.byName().get(name)
                    ),
                    name + " must keep its plant food support"
            );
        }
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        world.board().plant(column, row, plant);
        plant.place(world, column, row, game.getCurrentTick());
        return plant;
    }

    private List<Sun> producedSuns() {
        return world.getCollectibles().stream()
                .filter(Sun.class::isInstance)
                .map(Sun.class::cast)
                .filter(sun -> sun.getSource() == SunSource.PLANT)
                .toList();
    }
}
