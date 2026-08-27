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
import pvz.data.ZombieCsvLoader;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.HorizontalDirection;
import pvz.model.core.board.TileOverlayType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;
import pvz.model.entity.plant.projectile.PlantProjectileEmitter;
import pvz.model.entity.projectile.DirectionalProjectile;
import pvz.model.entity.projectile.PeaHeatState;
import pvz.model.entity.projectile.Projectile;
import pvz.model.entity.projectile.ProjectileFamily;
import pvz.model.entity.projectile.ProjectileHitLimit;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class TorchwoodBehaviorTest {

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
                new Random(97)
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
    void torchwoodBlueFlameLastsSixtySeconds() {
        Plant torchwood = placePlant("Torchwood", 4, 3);
        TorchwoodStateCapability state = torchwood.behaviorCapability(
                TorchwoodStateCapability.class
        );

        assertNotNull(state);
        assertTrue(PlantFoodSupport.isImplemented(torchwood.getSpec()));
        assertEquals(TorchwoodStage.NORMAL, state.getStage());

        assertTrue(torchwood.tryApplyPlantFood(game.getCurrentTick()));
        assertEquals(TorchwoodStage.BLUE_FLAME, state.getStage());

        game.advance(2L * Game.TICKS_PER_SECOND);

        assertFalse(torchwood.isPlantFoodActive(game.getCurrentTick()));
        assertTrue(torchwood.canApplyPlantFood(game.getCurrentTick()));
        assertEquals(TorchwoodStage.BLUE_FLAME, state.getStage());

        game.advance(58L * Game.TICKS_PER_SECOND);

        assertEquals(TorchwoodStage.NORMAL, state.getStage());
    }

    @Test
    void repeatedPlantFoodResetsBlueFlameToFreshSixtySeconds() {
        Plant torchwood = placePlant("Torchwood", 4, 3);
        TorchwoodStateCapability state = torchwood.behaviorCapability(
                TorchwoodStateCapability.class
        );

        assertTrue(torchwood.tryApplyPlantFood(game.getCurrentTick()));
        game.advance(30L * Game.TICKS_PER_SECOND);

        assertEquals(TorchwoodStage.BLUE_FLAME, state.getStage());
        assertTrue(torchwood.tryApplyPlantFood(game.getCurrentTick()));

        game.advance(59L * Game.TICKS_PER_SECOND);
        assertEquals(TorchwoodStage.BLUE_FLAME, state.getStage());

        game.advance(Game.TICKS_PER_SECOND);
        assertEquals(TorchwoodStage.NORMAL, state.getStage());
    }

    @Test
    void expiredBlueFlameReturnsToNormalFireMultiplier() {
        Plant torchwood = placePlant("Torchwood", 4, 3);
        assertTrue(torchwood.tryApplyPlantFood(game.getCurrentTick()));

        game.advance(60L * Game.TICKS_PER_SECOND);

        Zombie zombie = spawnZombie("ZombieDefault", 7, 3);
        double healthBefore = zombie.getHealth();
        Projectile pea = peaProjectile(
                2,
                3,
                ProjectileType.NORMAL,
                HorizontalDirection.RIGHT
        );

        runUntilHit(pea, zombie, 30);

        assertEquals(PeaHeatState.FIRE, pea.getPeaHeatState());
        assertEquals(2, pea.getPeaDamageMultiplier());
        assertEquals(healthBefore - 40, zombie.getHealth());
    }

    @Test
    void peaTaggedShooterEmitterCreatesTorchwoodCompatibleProjectiles() {
        placePlant("Torchwood", 4, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 7, 3);
        double healthBefore = zombie.getHealth();

        Plant peashooter = plantFactory.create("Peashooter");
        PlantProjectileEmitter emitter = new PlantProjectileEmitter(
                peashooter.getSpec()
        );
        emitter.onPlaced(world, 2);
        emitter.emit(
                3,
                0,
                20,
                ProjectileType.NORMAL,
                Integer.MAX_VALUE,
                HorizontalDirection.RIGHT
        );

        game.advance(30);

        assertEquals(healthBefore - 40, zombie.getHealth());
    }

    @Test
    void normalTorchwoodTurnsPeasIntoFireAndDoublesDamage() {
        placePlant("Torchwood", 4, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 7, 3);
        double healthBefore = zombie.getHealth();
        Projectile pea = peaProjectile(
                2,
                3,
                ProjectileType.NORMAL,
                HorizontalDirection.RIGHT
        );

        runUntilHit(pea, zombie, 30);

        assertEquals(PeaHeatState.FIRE, pea.getPeaHeatState());
        assertEquals(ProjectileType.FIRE, pea.getType());
        assertEquals(2, pea.getPeaDamageMultiplier());
        assertEquals(healthBefore - 40, zombie.getHealth());
    }

    @Test
    void multipleNormalTorchwoodsDoNotStackPeaDamage() {
        placePlant("Torchwood", 4, 3);
        placePlant("Torchwood", 5, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 7, 3);
        double healthBefore = zombie.getHealth();
        Projectile pea = peaProjectile(
                2,
                3,
                ProjectileType.NORMAL,
                HorizontalDirection.RIGHT
        );

        runUntilHit(pea, zombie, 30);

        assertEquals(PeaHeatState.FIRE, pea.getPeaHeatState());
        assertEquals(2, pea.getPeaDamageMultiplier());
        assertEquals(healthBefore - 40, zombie.getHealth());
    }

    @Test
    void blueTorchwoodTriplesPeaDamageAndCannotBeDowngraded() {
        Plant blueTorchwood = placePlant("Torchwood", 4, 3);
        placePlant("Torchwood", 5, 3);
        assertTrue(blueTorchwood.tryApplyPlantFood(game.getCurrentTick()));

        Zombie zombie = spawnZombie("ZombieDefault", 7, 3);
        double healthBefore = zombie.getHealth();
        Projectile pea = peaProjectile(
                2,
                3,
                ProjectileType.NORMAL,
                HorizontalDirection.RIGHT
        );

        runUntilHit(pea, zombie, 30);

        assertEquals(PeaHeatState.BLUE_FIRE, pea.getPeaHeatState());
        assertEquals(3, pea.getPeaDamageMultiplier());
        assertEquals(ProjectileType.FIRE, pea.getType());
        assertEquals(healthBefore - 60, zombie.getHealth());
    }

    @Test
    void snowPeaLosesIceEffectWhenPassingThroughTorchwood() {
        placePlant("Torchwood", 4, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 7, 3);
        double healthBefore = zombie.getHealth();
        Projectile snowPea = peaProjectile(
                2,
                3,
                ProjectileType.ICE,
                HorizontalDirection.RIGHT
        );

        runUntilHit(snowPea, zombie, 30);

        assertEquals(ProjectileType.FIRE, snowPea.getType());
        assertEquals(healthBefore - 40, zombie.getHealth());
        assertFalse(zombie.isChilled(30));
    }

    @Test
    void firePeashooterDoesNotBecomeFourTimesStrongerAtNormalTorchwood() {
        placePlant("Torchwood", 4, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 7, 3);
        double healthBefore = zombie.getHealth();
        Projectile firePea = peaProjectile(
                2,
                3,
                ProjectileType.FIRE,
                HorizontalDirection.RIGHT
        );

        runUntilHit(firePea, zombie, 30);

        assertEquals(PeaHeatState.FIRE, firePea.getPeaHeatState());
        assertEquals(2, firePea.getPeaDamageMultiplier());
        assertEquals(healthBefore - 40, zombie.getHealth());
    }

    @Test
    void directionalPeaProjectilesUseTheSameTorchwoodModifierPath() {
        placePlant("Torchwood", 4, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 7, 3);
        double healthBefore = zombie.getHealth();
        DirectionalProjectile pea = new DirectionalProjectile(
                world,
                "directional pea",
                2,
                3,
                0,
                20,
                ProjectileType.NORMAL,
                Integer.MAX_VALUE,
                ShotVector.RIGHT,
                ProjectileHitLimit.singleHit(),
                ProjectileFamily.PEA
        );

        for (int tick = 1; tick <= 30; tick++) {
            pea.update(tick);
            if (zombie.getHealth() < healthBefore) {
                break;
            }
        }

        assertEquals(PeaHeatState.FIRE, pea.getPeaHeatState());
        assertEquals(ProjectileType.FIRE, pea.getProjectileType());
        assertEquals(healthBefore - 40, zombie.getHealth());
    }

    @Test
    void nonPeaProjectilePassesTorchwoodUnchanged() {
        placePlant("Torchwood", 4, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 7, 3);
        double healthBefore = zombie.getHealth();
        Projectile generic = genericProjectile(
                2,
                3,
                ProjectileType.NORMAL,
                HorizontalDirection.RIGHT
        );

        runUntilHit(generic, zombie, 30);

        assertEquals(ProjectileFamily.GENERIC, generic.getProjectileFamily());
        assertEquals(PeaHeatState.UNHEATED, generic.getPeaHeatState());
        assertEquals(ProjectileType.NORMAL, generic.getType());
        assertEquals(healthBefore - 20, zombie.getHealth());
    }

    @Test
    void torchwoodAlsoModifiesBackwardPeas() {
        placePlant("Torchwood", 4, 3);
        Zombie zombie = spawnZombie("ZombieDefault", 2, 3);
        double healthBefore = zombie.getHealth();
        Projectile pea = peaProjectile(
                6,
                3,
                ProjectileType.NORMAL,
                HorizontalDirection.LEFT
        );

        runUntilHit(pea, zombie, 30);

        assertEquals(PeaHeatState.FIRE, pea.getPeaHeatState());
        assertEquals(healthBefore - 40, zombie.getHealth());
    }

    @Test
    void torchwoodFirePeaInstantlyMeltsFrozenPlantOverlay() {
        placePlant("Torchwood", 4, 3);
        Plant frozenPlant = placePlant("Peashooter", 6, 3);
        for (int level = 0; level < Plant.FULL_FREEZE_LEVEL; level++) {
            assertTrue(
                    world.board().addPlantFreezeLevel(
                            frozenPlant,
                            Plant.FULL_FREEZE_LEVEL
                    )
            );
        }
        assertTrue(
                world.board().getTile(6, 3)
                        .hasOverlay(TileOverlayType.FROZEN)
        );

        Projectile pea = peaProjectile(
                2,
                3,
                ProjectileType.NORMAL,
                HorizontalDirection.RIGHT
        );
        for (int tick = 1; tick <= 25; tick++) {
            pea.update(tick);
            if (!world.board().getTile(6, 3)
                    .hasOverlay(TileOverlayType.FROZEN)) {
                break;
            }
        }

        assertEquals(ProjectileType.FIRE, pea.getType());
        assertFalse(
                world.board().getTile(6, 3)
                        .hasOverlay(TileOverlayType.FROZEN)
        );
    }

    private Projectile peaProjectile(
            int startColumn,
            int row,
            ProjectileType type,
            HorizontalDirection direction
    ) {
        return new Projectile(
                world,
                "test pea",
                startColumn,
                row,
                0,
                20,
                type,
                Integer.MAX_VALUE,
                direction,
                ProjectileHitLimit.singleHit(),
                ProjectileFamily.PEA
        );
    }

    private Projectile genericProjectile(
            int startColumn,
            int row,
            ProjectileType type,
            HorizontalDirection direction
    ) {
        return new Projectile(
                world,
                "generic projectile",
                startColumn,
                row,
                0,
                20,
                type,
                Integer.MAX_VALUE,
                direction,
                ProjectileHitLimit.singleHit(),
                ProjectileFamily.GENERIC
        );
    }

    private void runUntilHit(
            Projectile projectile,
            Zombie zombie,
            int maximumTicks
    ) {
        double healthBefore = zombie.getHealth();

        for (int tick = 1; tick <= maximumTicks; tick++) {
            projectile.update(tick);
            if (zombie.getHealth() < healthBefore) {
                return;
            }
        }

        throw new AssertionError("projectile did not hit the target zombie");
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
}
