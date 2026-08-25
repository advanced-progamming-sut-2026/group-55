package pvz.model.entity.zombie;

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
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.Tile;
import pvz.model.core.board.TileOverlay;
import pvz.model.core.board.TileOverlayType;
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.lifecycle.PlantRemovalResult;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.behavior.ExplorerTorchBehavior;
import pvz.model.entity.zombie.behavior.LobbedShieldBehavior;
import pvz.model.entity.zombie.behavior.ProspectorBehavior;

class CombatInteractionRegressionTest {
    private Game game;
    private World world;
    private ZombieFactory zombieFactory;
    private PlantFactory plantFactory;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        world = new World(
                game,
                new Board(9, 5),
                new BattleResources(500, 0),
                new Random(1)
        );
        zombieFactory = new ZombieFactory(
                ZombieCsvLoader.load("assets/Data/zombies.csv")
        );
        plantFactory = new PlantFactory(
                PlantCsvLoader.load("assets/Data/plants.csv").byName()
        );
        world.setZombieCreator(id -> zombieFactory.create(id, 3));
        game.register(world.board());
    }

    @Test
    void lobbedAreaRetainsLobbedDeliveryForParasolAndSnorkel() {
        Zombie parasol = zombieFactory.create("ZombieLostCityJane", 3);
        parasol.spawn(world, 9, 1);
        double parasolHealth = parasol.getHealth();

        world.board().damageZombiesWithProjectileInArea(
                world.getZombies(),
                9,
                1,
                1,
                100,
                ProjectileType.NORMAL,
                DamageContext.AttackDelivery.LOBBED,
                0
        );

        assertEquals(parasolHealth, parasol.getHealth());

        world.board().setTileType(9, 2, TileType.WATER);
        Zombie snorkel = zombieFactory.create("ZombieBeachSnorkel", 3);
        snorkel.spawn(world, 9, 2);
        double snorkelHealth = snorkel.getHealth();

        world.board().damageZombiesWithProjectileInArea(
                world.getZombies(),
                9,
                2,
                0,
                100,
                ProjectileType.NORMAL,
                DamageContext.AttackDelivery.LOBBED,
                0
        );

        assertEquals(snorkelHealth - 100, snorkel.getHealth());
        double healthAfterLobbedHit = snorkel.getHealth();

        world.board().damageZombiesWithProjectileInArea(
                world.getZombies(),
                9,
                2,
                0,
                100,
                ProjectileType.NORMAL,
                DamageContext.AttackDelivery.STRAIGHT,
                0
        );

        assertEquals(healthAfterLobbedHit, snorkel.getHealth());
    }

    @Test
    void frozenOverlayPreservesTerrainAndCannotBeHealedByReapplication() {
        world.board().setTileType(4, 2, TileType.LOW_BEACH);
        Plant plant = placePlant("Peashooter", 4, 2);

        assertTrue(world.board().addPlantFreezeLevel(plant, 3));
        assertTrue(world.board().addPlantFreezeLevel(plant, 3));
        assertTrue(world.board().addPlantFreezeLevel(plant, 3));

        Tile tile = world.board().getTile(4, 2);
        assertEquals(TileType.LOW_BEACH, tile.getType());
        assertTrue(tile.hasOverlay(TileOverlayType.FROZEN));
        assertEquals(3, plant.getFreezeLevel());

        world.board().damageTerrain(4, 2, 100);
        assertEquals(500, frozenOverlay(tile).getRemainingHealth());

        assertFalse(world.board().addPlantFreezeLevel(plant, 3));
        assertEquals(500, frozenOverlay(tile).getRemainingHealth());

        world.board().damageTerrainWithProjectile(
                4,
                2,
                1,
                ProjectileType.FIRE
        );

        assertFalse(tile.hasOverlay(TileOverlayType.FROZEN));
        assertEquals(TileType.LOW_BEACH, tile.getType());
        assertEquals(0, plant.getFreezeLevel());
    }

    @Test
    void adjacentFireDamagesIceAndFirePlantsResistFreezeLevels() {
        Plant frozenPlant = placePlant("Peashooter", 4, 2);
        placePlant("Fire Peashooter", 3, 1);

        for (int level = 0; level < 3; level++) {
            assertTrue(world.board().addPlantFreezeLevel(frozenPlant, 3));
        }

        game.advance(Game.TICKS_PER_SECOND);

        assertEquals(
                540,
                frozenOverlay(world.board().getTile(4, 2))
                        .getRemainingHealth()
        );

        Plant firePlant = placePlant("Fire Peashooter", 6, 2);
        assertFalse(world.board().addPlantFreezeLevel(firePlant, 3));
        assertEquals(0, firePlant.getFreezeLevel());
    }

    @Test
    void octopusOverlayDoesNotReplaceTerrainOrHealWhenReapplied() {
        world.board().setTileType(5, 3, TileType.LOW_BEACH);
        Plant plant = placePlant("Peashooter", 5, 3);
        Tile tile = world.board().getTile(5, 3);

        assertTrue(world.board().coverPlantWithOctopus(plant));
        world.board().damageTerrain(5, 3, 100);
        assertFalse(world.board().coverPlantWithOctopus(plant));

        TileOverlay octopus = tile.getOverlays().stream()
                .filter(overlay -> overlay.getType()
                        == TileOverlayType.OCTOPUS)
                .findFirst()
                .orElseThrow();
        assertEquals(500, octopus.getRemainingHealth());
        assertEquals(TileType.LOW_BEACH, tile.getType());

        world.board().damageTerrainWithProjectile(
                5,
                3,
                250,
                ProjectileType.FIRE
        );

        assertFalse(tile.hasOverlay(TileOverlayType.OCTOPUS));
        assertEquals(TileType.LOW_BEACH, tile.getType());
    }

    @Test
    void reflectedIceAddsOneLevelAndReflectedFireMeltsFullIce() {
        Plant target = placePlant("Peashooter", 2, 1);
        Zombie juggler = zombieFactory.create("ZombieDarkJuggler", 3);
        juggler.spawn(world, 5, 1);
        double jugglerHealth = juggler.getHealth();

        for (int level = 1; level <= 3; level++) {
            ProjectileType.ICE.hitZombie(juggler, 20, level);
            assertEquals(level, target.getFreezeLevel());
        }

        assertEquals(jugglerHealth, juggler.getHealth());
        assertTrue(world.board().getTile(2, 1)
                .hasOverlay(TileOverlayType.FROZEN));
        double targetHealthBeforeFire = target.getHealth();

        ProjectileType.FIRE.hitZombie(juggler, 20, 4);

        assertFalse(world.board().getTile(2, 1)
                .hasOverlay(TileOverlayType.FROZEN));
        assertEquals(0, target.getFreezeLevel());
        assertEquals(targetHealthBeforeFire, target.getHealth());
    }

    @Test
    void frozenJugglerTakesStraightProjectileDamageWithoutReflecting() {
        Plant target = placePlant("Peashooter", 2, 1);
        Zombie juggler = zombieFactory.create("ZombieDarkJuggler", 3);
        juggler.spawn(world, 5, 1);
        juggler.applyFreeze(0, 20);
        double targetHealth = target.getHealth();
        double jugglerHealth = juggler.getHealth();

        ProjectileType.NORMAL.hitZombie(juggler, 100, 1);

        assertEquals(targetHealth, target.getHealth());
        assertEquals(jugglerHealth - 100, juggler.getHealth());
    }

    @Test
    void lobbedButterIsNotReflectedAndButterStopsLaterReflection() {
        Plant target = placePlant("Peashooter", 2, 2);
        Zombie juggler = zombieFactory.create("ZombieDarkJuggler", 3);
        juggler.spawn(world, 5, 2);
        double targetHealth = target.getHealth();
        double jugglerHealth = juggler.getHealth();

        ProjectileType.NORMAL.hitZombie(
                juggler,
                60,
                1,
                DamageContext.AttackDelivery.LOBBED
        );
        assertEquals(jugglerHealth - 60, juggler.getHealth());
        assertEquals(targetHealth, target.getHealth());

        juggler.applyButterStun(1, 20);
        ProjectileType.NORMAL.hitZombie(juggler, 100, 2);

        assertEquals(jugglerHealth - 160, juggler.getHealth());
        assertEquals(targetHealth, target.getHealth());
    }

    @Test
    void activePlantFoodBlocksAllHarmfulPlantEffects() {
        Plant plant = placePlant("Peashooter", 4, 3);
        assertTrue(plant.tryApplyPlantFood(0));
        double protectedHealth = plant.getHealth();

        plant.takeDamage(100);

        assertEquals(protectedHealth, plant.getHealth());
        assertFalse(world.board().addPlantFreezeLevel(
                plant,
                Plant.FULL_FREEZE_LEVEL
        ));
        assertFalse(world.board().coverPlantWithOctopus(plant));
        assertFalse(plant.addActionBlocker(new Object()));
        assertFalse(plant.tryRelocate(5, 3));
        assertEquals(
                PlantRemovalResult.BLOCKED_BY_PLANT_FOOD,
                plant.tryRemove(PlantThreat.INSTANT_DESTROY)
        );
        assertEquals(0, plant.getFreezeLevel());
        assertFalse(plant.isActionBlocked());
        assertEquals(4, plant.getTileX());
        assertTrue(world.board().getTile(4, 3).getOverlays().isEmpty());
    }

    @Test
    void frozenOrOctopusCoveredPlantCannotStartPlantFood() {
        Plant frozen = placePlant("Peashooter", 3, 4);
        assertTrue(world.board().addPlantFreezeLevel(
                frozen,
                Plant.FULL_FREEZE_LEVEL
        ));

        assertFalse(frozen.canApplyPlantFood(0));
        assertFalse(frozen.tryApplyPlantFood(0));
        assertFalse(frozen.isPlantFoodActive(0));

        Plant octopused = placePlant("Peashooter", 5, 4);
        assertTrue(world.board().coverPlantWithOctopus(octopused));

        assertFalse(octopused.canApplyPlantFood(0));
        assertFalse(octopused.tryApplyPlantFood(0));
        assertFalse(octopused.isPlantFoodActive(0));
    }

    @Test
    void blockedProjectileDoesNotApplyElementSideEffects() {
        Plant explorerTarget = placePlant("Peashooter", 4, 1);
        Zombie explorer = customZombie(
                "BlockedExplorer",
                List.of(
                        new ExplorerTorchBehavior(),
                        new LobbedShieldBehavior()
                )
        );
        explorer.spawn(world, 4, 1);

        ProjectileType.ICE.hitZombie(
                explorer,
                20,
                0,
                DamageContext.AttackDelivery.LOBBED
        );
        explorer.update(1);

        assertTrue(explorerTarget.isRemovedFromWorld());

        Zombie prospector = customZombie(
                "BlockedProspector",
                List.of(
                        new ProspectorBehavior(1, 1),
                        new LobbedShieldBehavior()
                )
        );
        prospector.spawn(world, 9, 2);

        ProjectileType.ICE.hitZombie(
                prospector,
                20,
                0,
                DamageContext.AttackDelivery.LOBBED
        );
        game.advance(Game.TICKS_PER_SECOND);

        assertEquals(1, prospector.getTileX());
    }

    @Test
    void acceptedIceAndFireHitsUpdateExplorerTorch() {
        Plant target = placePlant("Peashooter", 6, 4);
        Zombie explorer = customZombie(
                "ExplorerElementTest",
                List.of(new ExplorerTorchBehavior())
        );
        explorer.spawn(world, 6, 4);

        ProjectileType.ICE.hitZombie(explorer, 20, 0);
        explorer.update(1);
        assertFalse(target.isRemovedFromWorld());

        ProjectileType.FIRE.hitZombie(explorer, 20, 2);
        explorer.update(2);
        assertTrue(target.isRemovedFromWorld());
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        world.board().plant(column, row, plant);
        plant.place(world, column, row, game.getCurrentTick());
        game.register(plant);
        return plant;
    }

    private TileOverlay frozenOverlay(Tile tile) {
        return tile.getOverlays().stream()
                .filter(overlay -> overlay.getType()
                        == TileOverlayType.FROZEN)
                .findFirst()
                .orElseThrow();
    }

    private Zombie customZombie(
            String id,
            List<pvz.model.entity.zombie.behavior.ZombieBehavior> behaviors
    ) {
        ZombieSpec spec = new ZombieSpec(
                id,
                id,
                500,
                100,
                0.1,
                100,
                100,
                List.of(),
                List.of(),
                true
        );
        return new Zombie(spec, new ArmorSet(List.of()), behaviors);
    }
}
