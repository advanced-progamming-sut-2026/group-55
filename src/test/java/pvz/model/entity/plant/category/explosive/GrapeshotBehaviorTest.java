package pvz.model.entity.plant.category.explosive;

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
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class GrapeshotBehaviorTest {

    private static final int GRAPE_COUNT = 8;

    private static final int MAX_BOUNCES = 5;

    private static final long MAX_LIFETIME_TICKS = 50;

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
                new Random(31)
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
    void grapeshotExplodesInThreeByThreeAndLaunchesEightGrapes() {
        Zombie insideBlast = frozenZombie("ZombieDefault", 6, 3);
        Zombie outsideBlast = frozenZombie("ZombieDefault", 8, 3);

        Plant grapeshot = placePlant("Grapeshot", 5, 3);

        assertFalse(insideBlast.isDead());
        game.advance(5);

        assertTrue(insideBlast.isDead());
        assertFalse(outsideBlast.isDead());

        GrapeshotVolley volley = volley(grapeshot);
        assertEquals(GRAPE_COUNT, volley.getGrapeCount());
        assertEquals(GRAPE_COUNT, volley.getActiveGrapeCount());
    }

    @Test
    void everyGrapeMovesExactlyOneTilePerTick() {
        Plant grapeshot = placePlant("Grapeshot", 5, 3);
        game.advance(5);
        GrapeshotVolley volley = volley(grapeshot);

        game.advance(1);

        for (GrapeshotVolley.GrapeState state : volley.getGrapeStates()) {
            int columnDistance = Math.abs(state.column() - 5);
            int rowDistance = Math.abs(state.row() - 3);

            assertTrue(columnDistance <= 1 && rowDistance <= 1);
            assertTrue(columnDistance + rowDistance > 0);
            assertEquals(0, state.bounceCount());
        }
    }

    @Test
    void aCornerBounceCountsAsASingleBounce() {
        Plant grapeshot = placePlant("Grapeshot", 1, 1);
        game.advance(5);
        GrapeshotVolley volley = volley(grapeshot);

        game.advance(1);

        GrapeshotVolley.GrapeState cornerGrape = volley.getGrapeStates()
                .stream()
                .filter(state -> state.deltaColumn() == 1
                        && state.deltaRow() == 1)
                .findFirst()
                .orElseThrow();

        assertEquals(1, cornerGrape.bounceCount());
        assertEquals(2, cornerGrape.column());
        assertEquals(2, cornerGrape.row());
    }

    @Test
    void grapesDamageHostileTileContentButNeverPlants() {
        world.board().setTileType(7, 3, TileType.TOMBSTONE);
        Plant safePlant = placePlant("Peashooter", 3, 3);
        Zombie zombie = frozenZombie("ZombieGargantuar", 7, 5);
        double plantHealth = safePlant.getHealth();
        double zombieHealth = zombie.getHealth();

        placePlant("Grapeshot", 5, 3);

        game.advance(7);

        assertEquals(690, world.board().getTile(7, 3).getHealth());
        assertEquals(zombieHealth - 10, zombie.getHealth());
        assertEquals(plantHealth, safePlant.getHealth());
    }

    @Test
    void volleyRespectsBounceAndLifetimeCapsThenUnregisters() {
        Plant grapeshot = placePlant("Grapeshot", 5, 3);
        game.advance(5);
        GrapeshotVolley volley = volley(grapeshot);
        int registeredAfterActivation = game.getRegisteredObjectCount();

        game.advance(MAX_LIFETIME_TICKS);

        List<GrapeshotVolley.GrapeState> states = volley.getGrapeStates();

        for (GrapeshotVolley.GrapeState state : states) {
            assertTrue(state.bounceCount() <= MAX_BOUNCES);
        }

        assertEquals(0, volley.getActiveGrapeCount());
        assertEquals(
                registeredAfterActivation - 1,
                game.getRegisteredObjectCount()
        );
    }

    private GrapeshotVolley volley(Plant grapeshot) {
        return grapeshot.behaviorCapability(GrapeshotBehavior.class).volley();
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
        zombie.applyFreeze(game.getCurrentTick(), 400);
        return zombie;
    }
}
