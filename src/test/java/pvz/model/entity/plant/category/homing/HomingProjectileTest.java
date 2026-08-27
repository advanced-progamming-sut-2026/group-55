package pvz.model.entity.plant.category.homing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.projectile.homing.HomingImpact;
import pvz.model.entity.projectile.homing.HomingProjectile;
import pvz.model.entity.projectile.homing.ZombieHomingTarget;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class HomingProjectileTest {

    private Game game;
    private World world;
    private PlantFactory plantFactory;
    private ZombieFactory zombieFactory;
    private RecordingImpact impact;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        world = new World(
                game,
                new Board(9, 5),
                new BattleResources(1000, 0),
                new Random(17)
        );
        plantFactory = new PlantFactory(
                PlantCsvLoader.load("assets/Data/plants.csv").byName()
        );
        zombieFactory = new ZombieFactory(
                ZombieCsvLoader.load("assets/Data/zombies.csv")
        );
        world.setZombieCreator(id -> zombieFactory.create(id, 3));
        game.register(world.board());
        impact = new RecordingImpact();
        GameEvents.drain();
    }

    @Test
    void projectileTracksAZombieThatKeepsMoving() {
        Zombie zombie = spawnZombie("ZombieDefault", 9, 3);
        HomingProjectile projectile = launchAt(zombie, 0.5, 2.5);

        game.advance(1);
        double firstX = projectile.getX();

        assertTrue(firstX > 0.5);
        assertEquals(2.5, projectile.getY(), 1e-9);

        game.advance(5);

        assertTrue(projectile.getX() > firstX);
        assertFalse(projectile.isFinished());
    }

    @Test
    void projectileFollowsTheTargetIntoAnotherRow() {
        Zombie zombie = spawnZombie("ZombieDefault", 9, 1);
        zombie.applyFreeze(game.getCurrentTick(), 4000);
        HomingProjectile projectile = launchAt(zombie, 0.5, 4.5);

        game.advance(3);
        assertTrue(projectile.getY() < 4.5);

        zombie.moveToRow(5);
        double rowBeforeChase = projectile.getY();

        game.advance(3);

        assertTrue(projectile.getY() > rowBeforeChase);
    }

    @Test
    void projectileIsRemovedWhenItsTargetDiesAndNeverRetargets() {
        Zombie target = spawnZombie("ZombieDefault", 5, 3);
        target.applyFreeze(game.getCurrentTick(), 4000);
        Zombie bystander = spawnZombie("ZombieDefault", 8, 3);
        bystander.applyFreeze(game.getCurrentTick(), 4000);

        HomingProjectile projectile = launchAt(target, 0.5, 2.5);
        int registeredWithProjectile = game.getRegisteredObjectCount();

        target.takeDirectDamage(Double.MAX_VALUE);
        assertTrue(target.isDead());

        game.advance(1);

        assertTrue(projectile.isFinished());
        assertEquals(
                registeredWithProjectile - 2,
                game.getRegisteredObjectCount()
        );

        game.advance(60);

        assertTrue(impact.hitZombies.isEmpty());
        assertEquals(
                bystander.getMaximumHealth(),
                bystander.getHealth()
        );
    }

    @Test
    void projectileIgnoresZombiesAndGravesOnItsWay() {
        world.board().setTileType(4, 3, TileType.TOMBSTONE);
        Zombie blocker = spawnZombie("ZombieDefault", 6, 3);
        blocker.applyFreeze(game.getCurrentTick(), 4000);
        Zombie target = spawnZombie("ZombieDefault", 9, 3);
        target.applyFreeze(game.getCurrentTick(), 4000);
        Plant plant = placePlant("Wall-nut", 2, 3);
        double plantHealth = plant.getHealth();

        launchAt(target, 0.5, 2.5);

        game.advance(45);

        assertEquals(List.of(target), impact.hitZombies);
        assertEquals(
                blocker.getMaximumHealth(),
                blocker.getHealth()
        );
        assertEquals(
                700,
                world.board().getTile(4, 3).getHealth()
        );
        assertEquals(TileType.TOMBSTONE, world.board().getTile(4, 3).getType());
        assertEquals(plantHealth, plant.getHealth());
    }

    private HomingProjectile launchAt(
            Zombie zombie,
            double startX,
            double startY
    ) {
        HomingProjectile projectile = new HomingProjectile(
                world,
                "test homing shot",
                startX,
                startY,
                new ZombieHomingTarget(zombie),
                impact,
                HomingProjectile.DEFAULT_TILES_PER_SECOND
        );
        game.register(projectile);
        return projectile;
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

    private static final class RecordingImpact implements HomingImpact {

        private final List<Zombie> hitZombies = new ArrayList<>();

        @Override
        public void hitZombie(Zombie zombie, long currentTick) {
            hitZombies.add(zombie);
        }
    }
}
