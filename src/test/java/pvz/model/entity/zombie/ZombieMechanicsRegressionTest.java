package pvz.model.entity.zombie;

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
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.TileType;
import pvz.model.entity.collectible.sun.Sun;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.projectile.Projectile;
import pvz.model.entity.projectile.ProjectileType;

class ZombieMechanicsRegressionTest {
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
                new BattleResources(0, 0),
                new Random(2)
        );
        zombieFactory = new ZombieFactory(
                ZombieCsvLoader.load("assets/Data/zombies.csv")
        );
        plantFactory = new PlantFactory(
                PlantCsvLoader.load("assets/Data/plants.csv").byName()
        );
        world.setZombieCreator(id -> zombieFactory.create(id, 3));
    }

    @Test
    void gargantuarUsesSlowConfiguredSpeed() {
        Zombie gargantuar = zombieFactory.create("ZombieGargantuar", 3);
        assertEquals(0.12, gargantuar.getRuntimeStats().tilesPerSecond());
    }

    @Test
    void arcadeAndTroglobiteUseIndependentCsvConfiguredObstacles() {
        Zombie arcade = zombieFactory.create("ZombieArcade", 3);
        arcade.spawn(world, 9, 1);

        assertEquals(0, arcade.getArmorHealth());
        assertEquals(1, world.getPushedObstacles().size());
        PushedObstacle arcadeMachine = world.getPushedObstacles().getFirst();
        assertEquals("ARCADE_MACHINE", arcadeMachine.getId());
        assertEquals(1290, arcadeMachine.getMaximumHealth());
        assertEquals(
                arcade.getRuntimeStats().maxHealth(),
                arcadeMachine.getMaximumHealth()
        );
        assertEquals(8, arcadeMachine.getTileX());

        Zombie troglobite = zombieFactory.create(
                "ZombieIceAgeTroglobite",
                3
        );
        troglobite.spawn(world, 9, 2);

        assertEquals(0, troglobite.getArmorHealth());
        List<PushedObstacle> iceBlocks = world.getPushedObstacles().stream()
                .filter(obstacle -> obstacle.getId().equals("ICE_BLOCK"))
                .toList();
        assertEquals(3, iceBlocks.size());
        assertEquals(List.of(8, 7, 6), iceBlocks.stream()
                .map(PushedObstacle::getTileX)
                .toList());
        assertTrue(iceBlocks.stream().allMatch(
                obstacle -> obstacle.getMaximumHealth() == 600
        ));

        Zombie hardArcade = zombieFactory.create("ZombieArcade", 5);
        hardArcade.spawn(world, 9, 3);
        PushedObstacle scaledMachine = world.getPushedObstacles().stream()
                .filter(obstacle -> obstacle.getTileY() == 3)
                .findFirst()
                .orElseThrow();
        assertEquals(1290 * 5 / 3.0, scaledMachine.getMaximumHealth());
        assertEquals(
                hardArcade.getRuntimeStats().maxHealth(),
                scaledMachine.getMaximumHealth()
        );
    }

    @Test
    void straightShotsHitTheGroundObstacleBeforeItsZombie() {
        Zombie arcade = zombieFactory.create("ZombieArcade", 3);
        arcade.spawn(world, 9, 1);
        PushedObstacle machine = world.getPushedObstacles().getFirst();
        double zombieHealth = arcade.getHealth();

        Projectile projectile = new Projectile(
                world,
                "test shot",
                7,
                1,
                100,
                ProjectileType.NORMAL,
                3
        );
        for (int tick = 1; tick <= 5; tick++) {
            projectile.update(tick);
        }

        assertEquals(1190, machine.getHealth());
        assertEquals(zombieHealth, arcade.getHealth());

        ProjectileType.NORMAL.hitZombie(
                arcade,
                100,
                6,
                DamageContext.AttackDelivery.LOBBED
        );
        assertEquals(zombieHealth - 100, arcade.getHealth());
        assertEquals(1190, machine.getHealth());
    }

    @Test
    void destroyedIceBlockLeavesItsOriginalSlotEmpty() {
        Zombie troglobite = zombieFactory.create(
                "ZombieIceAgeTroglobite",
                3
        );
        troglobite.spawn(world, 9, 2);
        List<PushedObstacle> blocks = world.getPushedObstacles().stream()
                .filter(obstacle -> obstacle.getId().equals("ICE_BLOCK"))
                .toList();

        blocks.getFirst().takeDirectDamage(
                blocks.getFirst().getMaximumHealth()
        );
        troglobite.moveByTiles(-1);

        assertEquals(List.of(6, 5), blocks.stream()
                .filter(obstacle -> !obstacle.isDead())
                .map(PushedObstacle::getTileX)
                .toList());
    }

    @Test
    void fireInstantlyMeltsOnlyCsvConfiguredIceObstacles() {
        Zombie troglobite = zombieFactory.create(
                "ZombieIceAgeTroglobite",
                3
        );
        troglobite.spawn(world, 9, 2);
        PushedObstacle ice = world.getPushedObstacles().stream()
                .filter(obstacle -> obstacle.getId().equals("ICE_BLOCK"))
                .findFirst()
                .orElseThrow();

        ice.takeProjectileDamage(ProjectileType.FIRE, 1);

        assertTrue(ice.isDead());
        assertFalse(world.getPushedObstacles().contains(ice));

        Zombie arcade = zombieFactory.create("ZombieArcade", 3);
        arcade.spawn(world, 9, 3);
        PushedObstacle machine = world.getPushedObstacles().stream()
                .filter(obstacle -> obstacle.getId().equals(
                        "ARCADE_MACHINE"
                ))
                .findFirst()
                .orElseThrow();

        machine.takeProjectileDamage(ProjectileType.FIRE, 1);

        assertFalse(machine.isDead());
        assertEquals(1288, machine.getHealth());
    }

    @Test
    void pushedObstacleCrushesPlantsAndSurvivesItsZombie() {
        Plant victim = placePlant("Peashooter", 8, 1);
        Zombie arcade = zombieFactory.create("ZombieArcade", 3);
        arcade.spawn(world, 9, 1);
        PushedObstacle machine = world.getPushedObstacles().getFirst();

        assertTrue(victim.isRemovedFromWorld());

        arcade.takeDirectDamage(Double.MAX_VALUE);
        assertFalse(machine.isDead());
        assertTrue(world.getPushedObstacles().contains(machine));
        assertTrue(world.hasStraightTargetAhead(1, 1.5));

        machine.takeDirectDamage(machine.getMaximumHealth());
        assertTrue(machine.isDead());
        assertFalse(world.getPushedObstacles().contains(machine));
    }

    @Test
    void survivingGroundObstacleKeepsItsTileOccupied() {
        Zombie arcade = zombieFactory.create("ZombieArcade", 3);
        arcade.spawn(world, 9, 1);
        PushedObstacle machine = world.getPushedObstacles().getFirst();
        arcade.takeDirectDamage(Double.MAX_VALUE);

        Plant newPlant = plantFactory.create("Peashooter");
        String placement = world.board().plant(8, 1, newPlant);
        assertTrue(placement.contains("ground obstacle"));
        assertTrue(world.board().getTile(8, 1).getPlants().isEmpty());
        assertFalse(world.board().placeTombstone(8, 1));

        Plant movable = placePlant("Peashooter", 7, 1);
        assertFalse(world.board().movePlant(7, 1, 8, 1, movable));
        assertTrue(world.board().getTile(7, 1).getPlants().contains(movable));

        machine.takeDirectDamage(machine.getMaximumHealth());
        String afterDestruction = world.board().plant(8, 1, newPlant);
        assertTrue(afterDestruction.startsWith("planted "));
    }

    @Test
    void lawnMowerDestroysPushedGroundObstaclesInItsRow() {
        Zombie arcade = zombieFactory.create("ZombieArcade", 3);
        arcade.spawn(world, 9, 1);
        PushedObstacle machine = world.getPushedObstacles().getFirst();

        world.activateLawnMower(1);

        assertTrue(arcade.isDead());
        assertTrue(machine.isDead());
        assertTrue(world.getZombies().isEmpty());
        assertTrue(world.getPushedObstacles().isEmpty());
    }

    @Test
    void barrelRollerUsesCsvBarrelAndReleasesTwoImpsOnce() {
        Zombie barrelRoller = zombieFactory.create(
                "ZombieBarrelRoller",
                3
        );
        barrelRoller.spawn(world, 9, 2);

        assertEquals(190, barrelRoller.getMaximumHealth());
        assertEquals(1, world.getPushedObstacles().size());
        PushedObstacle barrel = world.getPushedObstacles().getFirst();
        assertEquals("BARREL", barrel.getId());
        assertEquals(1200, barrel.getMaximumHealth());
        assertEquals(8, barrel.getTileX());
        assertEquals(2, barrel.getTileY());

        barrel.takeDirectDamage(1200);

        List<Zombie> imps = world.getZombies().stream()
                .filter(zombie -> zombie.getSpec().getId().equals(
                        "ZombieImp"
                ))
                .sorted((left, right) -> Double.compare(
                        right.getX(),
                        left.getX()
                ))
                .toList();
        assertEquals(2, imps.size());
        assertEquals(barrel.getX(), imps.getFirst().getX());
        assertTrue(Math.abs(
                imps.getFirst().getX() - imps.get(1).getX() - 0.35
        ) < 0.0000001);
        assertTrue(imps.stream().allMatch(imp -> imp.getTileY() == 2));
        assertFalse(barrelRoller.isDead());

        barrel.takeDirectDamage(1200);
        long finalImpCount = world.getZombies().stream()
                .filter(zombie -> zombie.getSpec().getId().equals(
                        "ZombieImp"
                ))
                .count();
        assertEquals(2L, finalImpCount);
    }

    @Test
    void barrelSurvivesItsRollerAndCanReleaseImpsLater() {
        Zombie barrelRoller = zombieFactory.create(
                "ZombieBarrelRoller",
                3
        );
        barrelRoller.spawn(world, 9, 3);
        PushedObstacle barrel = world.getPushedObstacles().getFirst();

        barrelRoller.takeDirectDamage(Double.MAX_VALUE);

        assertFalse(barrel.isDead());
        assertTrue(world.getPushedObstacles().contains(barrel));
        assertTrue(world.getZombies().isEmpty());

        barrel.takeDirectDamage(Double.MAX_VALUE);

        assertEquals(2, world.getZombies().size());
        assertTrue(world.getZombies().stream().allMatch(
                zombie -> zombie.getSpec().getId().equals("ZombieImp")
        ));
    }

    @Test
    void lawnMowerAlsoRemovesImpsReleasedFromABarrel() {
        Zombie barrelRoller = zombieFactory.create(
                "ZombieBarrelRoller",
                3
        );
        barrelRoller.spawn(world, 9, 4);

        world.activateLawnMower(4);

        assertTrue(world.getPushedObstacles().isEmpty());
        assertTrue(world.getZombies().isEmpty());
        assertTrue(barrelRoller.isDead());
    }

    @Test
    void areaDamageHitsOnlyTheTopPlantInEachTile() {
        world.board().setTileType(4, 1, TileType.WATER);
        Plant lilyPad = placePlant("Lily Pad", 4, 1);
        Plant peashooter = placePlant("Peashooter", 4, 1);
        double lilyPadHealth = lilyPad.getHealth();
        double peashooterHealth = peashooter.getHealth();

        world.board().damagePlantsInArea(4, 1, 0, 80);

        assertEquals(lilyPadHealth, lilyPad.getHealth());
        assertEquals(peashooterHealth - 80, peashooter.getHealth());
    }

    @Test
    void reversedProspectorIsRemovedAfterLeavingTheRightEdge() {
        Zombie prospector = zombieFactory.create("ZombieProspector", 3);
        prospector.spawn(world, 9, 1);

        game.advance(700);

        assertFalse(world.getZombies().contains(prospector));
        assertEquals(0, game.getRegisteredObjectCount());
        assertFalse(prospector.isDead());
    }

    @Test
    void turquoiseDetectsPlantsInItsConfiguredRadius() {
        world.sunBank().add(500);
        placePlant("Peashooter", 5, 1);
        Zombie turquoise = zombieFactory.create("ZombieCrystalSkull", 3);
        turquoise.spawn(world, 5, 3);

        turquoise.update(0);
        turquoise.update(10);

        assertEquals(475, world.sunBank().getBalance());
    }

    @Test
    void fishermanTargetsTheTopPlantInsteadOfItsLilyPad() {
        world.board().setTileType(4, 1, TileType.WATER);
        Plant lilyPad = placePlant("Lily Pad", 4, 1);
        Plant peashooter = placePlant("Peashooter", 4, 1);
        Zombie fisherman = zombieFactory.create(
                "ZombieBeachFisherman",
                3
        );
        fisherman.spawn(world, 9, 1);

        fisherman.update(25);

        assertEquals(4, lilyPad.getTileX());
        assertEquals(5, peashooter.getTileX());
        assertTrue(world.board().getTile(4, 1)
                .getPlants().contains(lilyPad));
        assertTrue(world.board().getTile(5, 1)
                .getPlants().contains(peashooter));
    }

    @Test
    void wizardTransformsTheTopPlantInAStack() {
        world.board().setTileType(4, 2, TileType.WATER);
        Plant lilyPad = placePlant("Lily Pad", 4, 2);
        Plant peashooter = placePlant("Peashooter", 4, 2);
        Zombie wizard = zombieFactory.create("ZombieWizard", 3);
        wizard.spawn(world, 9, 5);

        wizard.update(40);

        assertTrue(peashooter.isActionBlocked());
        assertFalse(lilyPad.isActionBlocked());
    }

    @Test
    void hunterAndOctopusTargetTheTopPlantInAStack() {
        world.board().setTileType(6, 1, TileType.WATER);
        Plant hunterLilyPad = placePlant("Lily Pad", 6, 1);
        Plant hunterTarget = placePlant("Peashooter", 6, 1);

        Zombie hunter = zombieFactory.create("ZombieIceAgeHunter", 3);
        hunter.spawn(world, 9, 1);
        hunter.update(30);

        assertEquals(0, hunterLilyPad.getFreezeLevel());
        assertEquals(1, hunterTarget.getFreezeLevel());

        world.board().setTileType(4, 2, TileType.WATER);
        Plant octopusLilyPad = placePlant("Lily Pad", 4, 2);
        Plant octopusTarget = placePlant("Peashooter", 4, 2);

        Zombie octopus = zombieFactory.create("ZombieBeachOctopus", 3);
        octopus.spawn(world, 9, 2);
        octopus.update(40);

        assertFalse(world.board().isPlantCovered(octopusLilyPad));
        assertTrue(world.board().isPlantCovered(octopusTarget));
    }

    @Test
    void turquoiseTimersPauseDuringFreezeAndButter() {
        world.sunBank().add(500);
        placePlant("Peashooter", 5, 1);
        Zombie frozen = zombieFactory.create("ZombieCrystalSkull", 3);
        frozen.spawn(world, 5, 3);
        frozen.update(0);
        frozen.applyFreeze(1, 49);

        for (int tick = 1; tick <= 58; tick++) {
            frozen.update(tick);
        }
        assertEquals(500, world.sunBank().getBalance());
        frozen.update(59);
        assertEquals(475, world.sunBank().getBalance());

        frozen.takeDirectDamage(Double.MAX_VALUE);
        world.sunBank().add(25);
        Zombie buttered = zombieFactory.create("ZombieCrystalSkull", 3);
        buttered.spawn(world, 5, 4);
        buttered.update(100);
        buttered.applyButterStun(101, 49);

        for (int tick = 101; tick <= 158; tick++) {
            buttered.update(tick);
        }
        assertEquals(500, world.sunBank().getBalance());
        buttered.update(159);
        assertEquals(475, world.sunBank().getBalance());
    }

    @Test
    void impUsesTheHigherCsvEatingDamage() {
        Zombie basic = zombieFactory.create("ZombieDefault", 3);
        Zombie imp = zombieFactory.create("ZombieImp", 3);

        assertEquals(100, basic.getRuntimeStats().eatDamagePerSecond());
        assertEquals(200, imp.getRuntimeStats().eatDamagePerSecond());
        assertTrue(
                imp.getRuntimeStats().eatDamagePerSecond()
                        > basic.getRuntimeStats().eatDamagePerSecond()
        );
    }

    @Test
    void pianoStillCrushesWithoutASeparateObstacle() {

        Plant pianoVictim = placePlant("Peashooter", 8, 3);
        Zombie piano = zombieFactory.create("ZombiePiano", 3);
        piano.spawn(world, 8, 3);
        piano.update(0);

        assertTrue(pianoVictim.isRemovedFromWorld());
    }

    @Test
    void kingOnlyPromotesTheTrulyBasicZombie() {
        Zombie king = zombieFactory.create("ZombieDarkKing", 3);
        Zombie cone = zombieFactory.create("ZombieArmor1", 3);
        Zombie basic = zombieFactory.create("ZombieDefault", 3);
        king.spawn(world, 9, 3);
        cone.spawn(world, 8, 3);
        basic.spawn(world, 7, 3);

        king.update(25);

        assertTrue(basic.getArmorSet().hasArmor("CROWN"));
        assertTrue(basic.getArmorSet().hasArmor("SHOULDER_ARMOR"));
        assertFalse(cone.getArmorSet().hasArmor("CROWN"));
        assertFalse(cone.getArmorSet().hasArmor("SHOULDER_ARMOR"));
    }

    @Test
    void raPartiallyConsumesSunWithoutDestroyingTheRemainder() {
        Zombie ra = zombieFactory.create("ZombieRa", 3);
        ra.spawn(world, 9, 1);
        Sun first = addRecoveredSun(225, 4, 1);
        Sun second = addRecoveredSun(75, 5, 1);

        ra.update(0);

        assertTrue(first.isRemoved());
        assertFalse(second.isRemoved());
        assertEquals(50, second.getValue());

        ra.takeDirectDamage(Double.MAX_VALUE);

        assertEquals(250, world.sunBank().getBalance());
    }

    @Test
    void everySpawnPathPublishesZombieDiscovery() {
        List<String> discoveredIds = new ArrayList<>();
        world.setZombieDiscoveryListener(
                spec -> discoveredIds.add(spec.getId())
        );

        zombieFactory.create("ZombieDefault", 3).spawn(world, 9, 1);
        world.spawnZombie("ZombieImp", 9, 2);

        assertEquals(
                List.of("ZombieDefault", "ZombieImp"),
                discoveredIds
        );
    }

    private Plant placePlant(String name, int column, int row) {
        Plant plant = plantFactory.create(name);
        world.board().plant(column, row, plant);
        plant.place(world, column, row, game.getCurrentTick());
        return plant;
    }

    private Sun addRecoveredSun(
            int value,
            int column,
            int row
    ) {
        Sun sun = Sun.recovered(
                world,
                column - 0.5,
                row - 0.5,
                value
        );
        world.addCollectible(sun);
        game.register(sun);
        return sun;
    }
}
