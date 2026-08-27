package pvz.model.entity.plant.category.explosive;

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
import pvz.data.ZombieCsvLoader;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.behavior.capability.ArmableTrapCapability;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class PotatoMineBehaviorTest {

    private static final long MINE_ARM_TICKS = 150;

    private static final long PRIMAL_ARM_TICKS = 50;

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
                new Random(13)
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
    void zombieWalksOverAnUnarmedMineInsteadOfEatingIt() {
        Plant mine = placePlant("Potato Mine", 5, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 5, 3);
        double startX = zombie.getX();

        assertFalse(mine.canBeEatenByZombie());

        game.advance(20);

        assertEquals(300, mine.getHealth());
        assertFalse(mine.isRemovedFromWorld());
        assertTrue(zombie.getX() < startX);
        assertFalse(zombie.isEating());
    }

    @Test
    void armedMineExplodesWhenAZombieEntersItsTile() {
        Plant mine = placePlant("Potato Mine", 2, 3);

        game.advance(MINE_ARM_TICKS);

        Zombie zombie = spawnZombie("ZombieDefault", 2, 3);

        game.advance(1);

        assertTrue(zombie.isDead());
        assertEquals(
                TransientActionWindow.State.EFFECT_ACTIVE,
                window(mine).getState()
        );

        game.advance(5);

        assertTrue(mine.isRemovedFromWorld());
    }

    @Test
    void enemyContentPresentWhenArmingCompletesExplodesImmediately() {
        Plant mine = placePlant("Potato Mine", 5, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 5, 3);
        zombie.applyFreeze(game.getCurrentTick(), 400);

        game.advance(MINE_ARM_TICKS - 1);
        assertFalse(zombie.isDead());

        game.advance(1);

        assertTrue(zombie.isDead());
        assertTrue(window(mine).isEffectActive());
    }

    @Test
    void pushedIceBlockTakesDamageBeforeCrushingAnArmedMine() {
        Plant mine = placePlant("Potato Mine", 4, 3);

        game.advance(MINE_ARM_TICKS);
        GameEvents.drain();

        spawnZombie("ZombieIceAgeTroglobite", 6, 3);
        PushedObstacle survivingIceBlock = world.getPushedObstacles()
                .getFirst();

        assertTrue(
                window(mine).isEffectActive(),
                "the ice block must trigger the armed mine on contact"
        );
        assertEquals(2, world.getPushedObstacles().size());
        assertFalse(survivingIceBlock.isDead());
        assertFalse(mine.isRemovedFromWorld());
        assertTrue(
                GameEvents.drain().stream()
                        .noneMatch(event -> event.contains("crushed")),
                "the ice block must be destroyed before it crushes the mine"
        );

        game.advance(5);

        assertTrue(mine.isRemovedFromWorld());
    }

    @Test
    void primalMineExplodesInAClippedThreeByThreeArea() {
        Plant mine = placePlant("Primal Potato Mine", 9, 1);
        Zombie onMine = spawnZombie("ZombieDefault", 9, 1);
        onMine.applyFreeze(game.getCurrentTick(), 400);
        Zombie neighbourColumn = spawnZombie("ZombieDefault", 8, 1);
        neighbourColumn.applyFreeze(game.getCurrentTick(), 400);
        Zombie neighbourRow = spawnZombie("ZombieDefault", 9, 2);
        neighbourRow.applyFreeze(game.getCurrentTick(), 400);
        Zombie farZombie = spawnZombie("ZombieDefault", 9, 4);
        farZombie.applyFreeze(game.getCurrentTick(), 400);

        game.advance(PRIMAL_ARM_TICKS);

        assertTrue(onMine.isDead());
        assertTrue(neighbourColumn.isDead());
        assertTrue(neighbourRow.isDead());
        assertFalse(farZombie.isDead());
        assertTrue(window(mine).isEffectActive());
    }

    @Test
    void plantFoodArmsTheMineAndPlantsTwoArmedClones() {
        Plant mine = placePlant("Potato Mine", 5, 3);

        assertTrue(mine.tryApplyPlantFood(game.getCurrentTick()));

        ArmableTrapCapability original = trap(mine);
        assertNotNull(original);
        assertTrue(original.isArmed(game.getCurrentTick()));

        List<Plant> mines = minesOnBoard();
        assertEquals(3, mines.size());

        for (Plant clone : mines) {
            assertTrue(
                    trap(clone).isArmed(game.getCurrentTick()),
                    "every mine clone must be armed"
            );
        }

        game.advance(40);

        assertEquals(3, minesOnBoard().size());
    }

    @Test
    void plantFoodArmingChecksTheCurrentTileImmediately() {
        Plant mine = placePlant("Potato Mine", 5, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 5, 3);
        zombie.applyFreeze(game.getCurrentTick(), 400);

        assertTrue(mine.tryApplyPlantFood(game.getCurrentTick()));

        assertTrue(zombie.isDead());
        assertTrue(window(mine).isEffectActive());

        game.advance(4);
        assertFalse(mine.isRemovedFromWorld());

        game.advance(1);
        assertTrue(mine.isRemovedFromWorld());
    }

    @Test
    void clonePlacementVisitsEveryCandidateTileWithoutRepeating() {
        game = new Game();
        world = new World(
                game,
                new Board(9, 5),
                new BattleResources(1000, 0),
                new ZeroRandom()
        );
        game.register(world.board());

        for (int column = 1; column <= 9; column++) {
            for (int row = 1; row <= 5; row++) {
                if ((column == 5 && row == 3)
                        || (column == 8 && row == 5)
                        || (column == 9 && row == 5)) {
                    continue;
                }

                Plant blocker = plantFactory.create("Peashooter");
                world.board().plant(column, row, blocker);
                blocker.place(world, column, row, game.getCurrentTick());
            }
        }

        Plant mine = placePlant("Potato Mine", 5, 3);

        assertTrue(mine.tryApplyPlantFood(game.getCurrentTick()));
        assertEquals(3, minesOnBoard().size());
    }

    private List<Plant> minesOnBoard() {
        return world.getPlants().stream()
                .filter(plant -> "Potato Mine".equals(plant.getName()))
                .toList();
    }

    private ArmableTrapCapability trap(Plant plant) {
        return plant.behaviorCapability(ArmableTrapCapability.class);
    }

    private TransientActionWindow window(Plant plant) {
        return plant.behaviorCapability(AbstractExplosiveBehavior.class)
                .window();
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

    private static final class ZeroRandom extends Random {

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }

}
