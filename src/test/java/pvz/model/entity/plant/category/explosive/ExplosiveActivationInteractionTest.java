package pvz.model.entity.plant.category.explosive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pvz.data.PlantCsvLoader;
import pvz.data.ZombieCsvLoader;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.behavior.capability.ArmableTrapCapability;
import pvz.model.entity.plant.hit.PlantHitSource;
import pvz.model.entity.plant.lifecycle.PlantRemovalResult;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class ExplosiveActivationInteractionTest {

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
                new BattleResources(1000, 3),
                new Random(37)
        );
        plantFactory = new PlantFactory(
                PlantCsvLoader.load("assets/Data/plants.csv").byName()
        );
        zombieFactory = new ZombieFactory(
                ZombieCsvLoader.load("assets/Data/zombies.csv")
        );
        world.setZombieCreator(id -> zombieFactory.create(id, 3));
        game.register(world.board());
    }

    @Test
    void cherryBombResolvesOnlyAtTheEndOfItsFiveTickActivation() {
        Zombie zombie = spawnZombie("ZombieDefault", 4, 3);
        Plant cherryBomb = placePlant("Cherry Bomb", 4, 3);

        assertTrue(cherryBomb.canBeEatenByZombie());
        assertFalse(zombie.isDead());

        game.advance(4);

        assertFalse(zombie.isDead());
        assertFalse(cherryBomb.isRemovedFromWorld());

        game.advance(1);

        assertTrue(zombie.isDead());
        assertTrue(cherryBomb.isRemovedFromWorld());
    }

    @Test
    void activatingCherryRejectsDamageAndInstantDestroyButStaysTargetable() {
        Plant cherryBomb = placePlant("Cherry Bomb", 4, 3);
        Zombie attacker = spawnZombie("ZombieGargantuar", 4, 3);
        double healthBefore = cherryBomb.getHealth();
        double xBefore = attacker.getX();

        assertTrue(cherryBomb.isZombieTargetable());
        assertTrue(cherryBomb.canBeEatenByZombie());
        assertFalse(cherryBomb.receiveHit(
                PlantHitSource.BITE,
                attacker,
                10_000
        ));
        assertEquals(healthBefore, cherryBomb.getHealth());
        assertEquals(
                PlantRemovalResult.BLOCKED_BY_ACTIVATION,
                cherryBomb.tryRemove(PlantThreat.INSTANT_DESTROY)
        );

        game.advance(1);

        assertFalse(cherryBomb.isRemovedFromWorld());
        assertEquals(xBefore, attacker.getX());
    }

    @Test
    void triggeredMineIsNeverEdibleAndDoesNotBlockASurvivingZombie() {
        Plant mine = placePlant("Potato Mine", 5, 3);
        ArmableTrapCapability trap = mine.behaviorCapability(
                ArmableTrapCapability.class
        );
        trap.armImmediately(game.getCurrentTick());

        Zombie gargantuar = spawnZombie("ZombieGargantuar", 5, 3);
        double xBefore = gargantuar.getX();

        game.advance(1);

        assertTrue(window(mine).isEffectActive());
        assertFalse(mine.canBeEatenByZombie());
        assertFalse(mine.canApplyPlantFood(game.getCurrentTick()));
        assertFalse(gargantuar.isDead());
        assertTrue(gargantuar.getX() < xBefore);
        assertFalse(mine.isRemovedFromWorld());
    }

    @Test
    void squashCannotReceivePlantFoodAfterItCommitsToItsAttack() {
        world.board().setTileType(6, 3, TileType.TOMBSTONE);
        Plant squash = placePlant("Squash", 5, 3);

        assertTrue(squash.canApplyPlantFood(game.getCurrentTick()));

        game.advance(1);

        assertTrue(window(squash).isEffectActive());
        assertFalse(squash.canApplyPlantFood(game.getCurrentTick()));
        assertFalse(squash.tryApplyPlantFood(game.getCurrentTick()));
    }


    @Test
    void contactOneShotsRejectPlantFoodAfterTheyTrigger() {
        Plant iceberg = placePlant("Iceberg Lettuce", 3, 2);
        spawnZombie("ZombieDefault", 3, 2);
        game.advance(1);

        assertTrue(window(iceberg).isEffectActive());
        assertFalse(iceberg.canApplyPlantFood(game.getCurrentTick()));

        world.board().setTileType(7, 4, TileType.WATER);
        Plant kelp = placePlant("Tangle Kelp", 7, 4);
        spawnZombie("ZombieDefault", 7, 4);
        game.advance(1);

        assertTrue(window(kelp).isEffectActive());
        assertFalse(kelp.canApplyPlantFood(game.getCurrentTick()));
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
}
