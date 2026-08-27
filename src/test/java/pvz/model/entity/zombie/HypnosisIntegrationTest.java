package pvz.model.entity.zombie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.category.lobber.LobberShot;
import pvz.model.entity.plant.category.lobber.LobberTarget;
import pvz.model.entity.projectile.LobbedProjectile;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.wave.Wave;
import pvz.model.wave.WaveManager;
import pvz.model.wave.WaveZombieEntry;

class HypnosisIntegrationTest {

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
                new BattleResources(1000, 0),
                new Random(17)
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
    void hypnosisReindexesTheSameZombieWithoutLosingState() {
        Zombie zombie = spawn("ZombieArmor1", 5, 3);
        zombie.takeAbilityDamage(
                25,
                DamageContext.ImpactMode.SINGLE_TARGET
        );
        double health = zombie.getHealth();
        double armor = zombie.getArmorHealth();
        double x = zombie.getX();

        assertTrue(HypnosisService.hypnotize(zombie, 0));

        assertEquals(1, world.getZombies().size());
        assertSame(zombie, world.getZombies().get(0));
        assertTrue(world.getHostileZombies().isEmpty());
        assertEquals(List.of(zombie), world.getAlliedZombies());
        assertEquals(health, zombie.getHealth());
        assertEquals(armor, zombie.getArmorHealth());
        assertEquals(x, zombie.getX());
    }

    @Test
    void playerAreaDamageNeverHitsAlliedZombie() {
        Zombie ally = spawn("ZombieDefault", 4, 3);
        HypnosisService.hypnotize(ally, 0);
        Zombie hostile = spawn("ZombieDefault", 4, 3);
        double allyHealth = ally.getHealth();
        double hostileHealth = hostile.getHealth();

        world.damageEnemyContentsInArea(4, 3, 1, 20);

        assertEquals(allyHealth, ally.getHealth());
        assertTrue(hostile.getHealth() < hostileHealth);
    }

    @Test
    void physicalOccupancyQueriesStillSeeAlliedZombies() {
        Zombie ally = spawn("ZombieDefault", 4, 3);
        HypnosisService.hypnotize(ally, 0);

        assertSame(ally, world.findZombieInTile(4, 3));
    }

    @Test
    void rowHeatingCanClearColdFromAnAlliedZombie() {
        Zombie ally = spawn("ZombieDefault", 4, 3);
        ally.applyFreeze(0, 50);
        HypnosisService.hypnotize(ally, 0);
        assertTrue(ally.isFrozen(0));

        world.clearZombieColdEffectsInRow(3, 0);

        assertFalse(ally.isFrozen(0));
    }

    @Test
    void opposingZombiesStopAndDamageEachOtherThroughArmor() {
        Zombie ally = spawn("ZombieDefault", 5, 3);
        HypnosisService.hypnotize(ally, 0);
        Zombie hostile = spawn("ZombieArmor1", 5, 3);
        double allyHealth = ally.getHealth();
        double hostileHealth = hostile.getHealth();
        double hostileArmor = hostile.getArmorHealth();

        game.advance(Game.TICKS_PER_SECOND + 1);

        assertTrue(ally.isFightingZombie());
        assertTrue(hostile.isFightingZombie());
        assertTrue(ally.getHealth() < allyHealth);
        assertTrue(hostile.getArmorHealth() < hostileArmor);
        assertEquals(hostileHealth, hostile.getHealth());
    }

    @Test
    void alliedHunterDoesNotUseItsPlantAttack() {
        Plant wallNut = placePlant("Wall-nut", 3, 3);
        Zombie hunter = spawn("ZombieIceAgeHunter", 4, 3);
        HypnosisService.hypnotize(hunter, 0);

        game.advance(80);

        assertEquals(0, wallNut.getFreezeLevel());
    }

    @Test
    void alliedGargantuarDoesNotThrowAnImp() {
        Zombie gargantuar = spawn("ZombieGargantuar", 6, 3);
        gargantuar.takeDirectDamage(
                gargantuar.getMaximumHealth() * 0.6
        );
        HypnosisService.hypnotize(gargantuar, 0);

        game.advance(20);

        assertTrue(world.getHostileZombies().isEmpty());
        assertEquals(List.of(gargantuar), world.getZombies());
    }

    @Test
    void hypnotizingRaReturnsSunItAlreadyStole() {
        Sun sun = Sun.recovered(world, 4.5, 2.5, 100);
        world.addCollectible(sun);
        game.register(sun);
        Zombie ra = spawn("ZombieRa", 7, 3);

        game.advance(1);
        assertTrue(sun.isRemoved());
        assertEquals(1000, world.sunBank().getBalance());

        HypnosisService.hypnotize(ra, game.getCurrentTick());

        assertEquals(1100, world.sunBank().getBalance());
    }

    @Test
    void hypnotizingWizardRestoresPlantsItTransformed() {
        Plant wallNut = placePlant("Wall-nut", 3, 3);
        Zombie wizard = spawn("ZombieWizard", 8, 3);

        game.advance(41);
        assertTrue(wallNut.isActionBlocked());

        HypnosisService.hypnotize(wizard, game.getCurrentTick());

        assertFalse(wallNut.isActionBlocked());
    }

    @Test
    void hypnotizingTurquoiseCancelsChargeAndReleasesHeldSun() {
        Plant wallNut = placePlant("Wall-nut", 4, 3);
        Zombie turquoise = spawn("ZombieCrystalSkull", 7, 3);

        game.advance(11);
        assertEquals(975, world.sunBank().getBalance());

        HypnosisService.hypnotize(turquoise, game.getCurrentTick());

        List<Sun> recovered = world.getCollectibles().stream()
                .filter(Sun.class::isInstance)
                .map(Sun.class::cast)
                .toList();
        assertEquals(1, recovered.size());
        assertEquals(12, recovered.get(0).getValue());

        game.advance(70);
        assertFalse(wallNut.isRemovedFromWorld());
        assertEquals(975, world.sunBank().getBalance());
    }

    @Test
    void alliedMovementDoesNotTriggerHostileContactPlants() {
        Plant iceberg = placePlant("Iceberg Lettuce", 6, 3);
        Zombie ally = spawn("ZombieDefault", 5, 3);
        HypnosisService.hypnotize(ally, 0);

        game.advance(40);

        assertFalse(ally.isFrozen(game.getCurrentTick()));
        assertFalse(iceberg.isRemovedFromWorld());
    }

    @Test
    void lawnMowerKillsOnlyHostileZombies() {
        Zombie ally = spawn("ZombieDefault", 2, 2);
        HypnosisService.hypnotize(ally, 0);
        Zombie hostile = spawn("ZombieDefault", 2, 2);

        world.activateLawnMower(2);

        assertFalse(ally.isDead());
        assertTrue(hostile.isDead());
    }

    @Test
    void finalWaveCompletesWhenItsLastEnemyIsHypnotized() {
        WaveManager waveManager = finalWaveManager("ZombieDefault");
        game.register(waveManager);
        waveManager.start(0);
        game.advance(1);
        Zombie zombie = world.getHostileZombies().get(0);

        HypnosisService.hypnotize(zombie, game.getCurrentTick());
        game.advance(1);

        assertTrue(waveManager.isCompleted());
    }

    @Test
    void stationaryKingCannotSoftlockFinalWaveAfterHypnosis() {
        WaveManager waveManager = finalWaveManager("ZombieDarkKing");
        game.register(waveManager);
        waveManager.start(0);
        game.advance(1);
        Zombie king = world.getHostileZombies().get(0);

        HypnosisService.hypnotize(king, game.getCurrentTick());
        game.advance(10);

        assertTrue(waveManager.isCompleted());
    }

    @Test
    void chargedAllstarInstantlyDestroysOpposingAlliedZombie() {
        Zombie ally = spawn("ZombieArmor2", 5, 3);
        HypnosisService.hypnotize(ally, 0);
        spawn("ZombieModernAllStar", 5, 3);

        game.advance(1);

        assertTrue(ally.isDead());
    }

    @Test
    void alliedAllstarUsesGenericCombatInsteadOfHostileChargeAbility() {
        Zombie allstar = spawn("ZombieModernAllStar", 5, 3);
        HypnosisService.hypnotize(allstar, 0);
        Zombie hostile = spawn("ZombieDarkArmor3", 5, 3);
        double hostileVitality = hostile.getHealth() + hostile.getArmorHealth();

        game.advance(1);

        assertFalse(hostile.isDead());
        assertEquals(
                hostileVitality,
                hostile.getHealth() + hostile.getArmorHealth()
        );
    }

    @Test
    void gargantuarSmashInstantlyDestroysOpposingAlliedZombie() {
        Zombie ally = spawn("ZombieDarkArmor3", 5, 3);
        HypnosisService.hypnotize(ally, 0);
        spawn("ZombieGargantuar", 5, 3);

        game.advance(1);

        assertTrue(ally.isDead());
    }

    @Test
    void arcadeMachineCrushesOpposingAlliedZombie() {
        Zombie ally = spawn("ZombieDefault", 5, 3);
        HypnosisService.hypnotize(ally, 0);

        spawn("ZombieArcade", 6, 3);

        assertTrue(ally.isDead());
    }

    @Test
    void troglobiteIceBlockCrushesOpposingAlliedZombie() {
        Zombie ally = spawn("ZombieDefault", 5, 3);
        HypnosisService.hypnotize(ally, 0);

        spawn("ZombieIceAgeTroglobite", 6, 3);

        assertTrue(ally.isDead());
    }

    @Test
    void chillDelaysAnAlreadyScheduledZombieCombatAttack() {
        Zombie ally = spawn("ZombieDefault", 5, 3);
        HypnosisService.hypnotize(ally, 0);
        Zombie hostile = spawn("ZombieDefault", 5, 3);

        game.advance(1);
        double hostileHealth = hostile.getHealth();
        ally.applyChill(game.getCurrentTick(), 100);

        game.advance(Game.TICKS_PER_SECOND);
        assertEquals(hostileHealth, hostile.getHealth());

        game.advance(Game.TICKS_PER_SECOND);
        assertTrue(hostile.getHealth() < hostileHealth);
    }

    @Test
    void removingChillAcceleratesAnAlreadyScheduledZombieCombatAttack() {
        Zombie ally = spawn("ZombieDefault", 5, 3);
        HypnosisService.hypnotize(ally, 0);
        Zombie hostile = spawn("ZombieDefault", 5, 3);

        game.advance(1);
        ally.applyChill(game.getCurrentTick(), 100);
        game.advance(1);
        double hostileHealth = hostile.getHealth();
        ally.removeChill(game.getCurrentTick());

        game.advance(Game.TICKS_PER_SECOND - 1);
        assertEquals(hostileHealth, hostile.getHealth());

        game.advance(1);
        assertTrue(hostile.getHealth() < hostileHealth);
    }

    @Test
    void lobbedProjectileDoesNotHitTargetHypnotizedDuringFlight() {
        Zombie zombie = spawn("ZombieDefault", 7, 3);
        double healthBefore = zombie.getHealth();
        LobbedProjectile projectile = new LobbedProjectile(
                world,
                "test-lob",
                2,
                3,
                LobberTarget.zombie(zombie),
                new LobberShot(40, 0, ProjectileType.NORMAL, 0),
                game.getCurrentTick()
        );
        game.register(projectile);

        game.advance(1);
        HypnosisService.hypnotize(zombie, game.getCurrentTick());
        game.advance(50);

        assertTrue(zombie.isAllied());
        assertEquals(healthBefore, zombie.getHealth());
    }

    private WaveManager finalWaveManager(String zombieId) {
        Wave wave = new Wave(
                1,
                List.of(new WaveZombieEntry(zombieId, 3, 100)),
                0,
                0,
                true
        );
        return new WaveManager(
                world,
                zombieFactory,
                List.of(wave),
                3
        );
    }

    private Zombie spawn(String id, int column, int row) {
        Zombie zombie = zombieFactory.create(id, 3);
        zombie.spawn(world, column, row);
        return zombie;
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        world.board().plant(column, row, plant);
        plant.place(world, column, row, game.getCurrentTick());
        game.register(plant);
        return plant;
    }
}
