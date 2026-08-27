package pvz.model.entity.plant.category.homing;

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
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.HypnosisService;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieAllegiance;
import pvz.model.entity.zombie.ZombieFactory;

class ElectricBlueberryAndCaulipowerTest {

    private static final long FIRST_SHOT_TICKS = 120;

    private Game game;
    private World world;
    private PlantFactory plantFactory;
    private ZombieFactory zombieFactory;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        world = newWorld(29);
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
    void electricBlueberryOnlyStrikesZombiesWhileAnyIsAlive() {
        placePlant("Electric Blueberry", 6, 3);
        for (int row = 1; row <= 5; row++) {
            world.board().setTileType(2, row, TileType.TOMBSTONE);
        }
        Zombie zombie = frozenZombie("ZombieDefault", 4, 3);

        game.advance(FIRST_SHOT_TICKS + 20);

        assertTrue(zombie.isDead());
        for (int row = 1; row <= 5; row++) {
            assertEquals(700, world.board().getTile(2, row).getHealth());
        }
    }

    @Test
    void electricBlueberryFallsBackToDestructibleContentWithoutZombies() {
        placePlant("Electric Blueberry", 6, 3);
        world.board().setTileType(3, 3, TileType.TOMBSTONE);

        game.advance(FIRST_SHOT_TICKS + 25);

        assertEquals(
                TileType.NORMAL,
                world.board().getTile(3, 3).getType()
        );
    }

    @Test
    void randomTargetSelectionIsDeterministicWithTheWorldRng() {
        assertEquals(
                firstElectricVictimIndex(),
                firstElectricVictimIndex()
        );
    }

    @Test
    void abilityDamageConsumesArmorBeforeHealth() {
        Zombie coneHead = frozenZombie("ZombieArmor1", 4, 3);
        double armorBefore = coneHead.getArmorHealth();
        double healthBefore = coneHead.getHealth();

        coneHead.takeAbilityDamage(
                15,
                DamageContext.AttackDelivery.HOMING,
                DamageContext.ImpactMode.SINGLE_TARGET
        );

        assertEquals(armorBefore - 15, coneHead.getArmorHealth());
        assertEquals(healthBefore, coneHead.getHealth());
    }

    @Test
    void electricBlueberryPlantFoodStrikesThreeDistinctZombies() {
        Plant blueberry = placePlant("Electric Blueberry", 6, 3);
        List<Zombie> zombies = List.of(
                frozenZombie("ZombieDefault", 2, 1),
                frozenZombie("ZombieDefault", 2, 2),
                frozenZombie("ZombieDefault", 3, 4),
                frozenZombie("ZombieDefault", 3, 5),
                frozenZombie("ZombieDefault", 4, 3)
        );

        assertTrue(blueberry.tryApplyPlantFood(game.getCurrentTick()));

        game.advance(40);

        assertEquals(
                3,
                zombies.stream().filter(Zombie::isDead).count()
        );
    }

    @Test
    void caulipowerHypnotizesOnlyWhenTheProjectileArrives() {
        placePlant("Caulipower", 6, 3);
        Zombie zombie = frozenZombie("ZombieDefault", 2, 3);

        game.advance(FIRST_SHOT_TICKS + 1);

        assertTrue(zombie.isHostile());

        game.advance(25);

        assertTrue(zombie.isAllied());
        assertFalse(zombie.isDead());
        assertEquals(zombie.getMaximumHealth(), zombie.getHealth());
        assertTrue(world.getCollectibles().isEmpty());
    }

    @Test
    void caulipowerNeverTargetsTerrainOrObstacles() {
        placePlant("Caulipower", 6, 3);
        world.board().setTileType(3, 3, TileType.TOMBSTONE);

        game.advance(3 * FIRST_SHOT_TICKS);

        assertEquals(700, world.board().getTile(3, 3).getHealth());
        assertEquals(
                TileType.TOMBSTONE,
                world.board().getTile(3, 3).getType()
        );
    }

    @Test
    void caulipowerPlantFoodHypnotizesUpToFiveZombies() {
        Plant caulipower = placePlant("Caulipower", 6, 3);
        List<Zombie> zombies = List.of(
                frozenZombie("ZombieDefault", 1, 1),
                frozenZombie("ZombieDefault", 1, 2),
                frozenZombie("ZombieDefault", 1, 3),
                frozenZombie("ZombieDefault", 1, 4),
                frozenZombie("ZombieDefault", 1, 5),
                frozenZombie("ZombieDefault", 2, 3)
        );

        assertTrue(caulipower.tryApplyPlantFood(game.getCurrentTick()));

        game.advance(60);

        assertEquals(
                5,
                zombies.stream().filter(Zombie::isAllied).count()
        );
    }

    @Test
    void hypnotizedZombiesAreNoLongerTargetedByPlants() {
        Zombie zombie = frozenZombie("ZombieDefault", 5, 3);

        assertTrue(HypnosisService.hypnotize(zombie, game.getCurrentTick()));
        assertFalse(HypnosisService.hypnotize(zombie, game.getCurrentTick()));

        assertEquals(ZombieAllegiance.ALLIED, zombie.getAllegiance());
        assertTrue(world.getHostileZombies().isEmpty());
        assertEquals(List.of(zombie), world.getAlliedZombies());
        assertFalse(world.hasZombieAhead(3, 0));
        assertFalse(world.hasStraightTargetAhead(3, 0));
        assertTrue(
                HomingTargetResolver.hostileZombieTargets(world).isEmpty()
        );
    }

    @Test
    void hypnotizedZombieWalksRightAndIgnoresPlants() {
        Plant wallNut = placePlant("Wall-nut", 3, 3);
        Zombie zombie = zombieFactory.create("ZombieDefault", 3);
        zombie.spawn(world, 4, 3);
        HypnosisService.hypnotize(zombie, game.getCurrentTick());

        double startX = zombie.getX();
        double wallNutHealth = wallNut.getHealth();

        game.advance(30);

        assertTrue(zombie.getX() > startX);
        assertEquals(wallNutHealth, wallNut.getHealth());
        assertFalse(zombie.isEating());
    }

    @Test
    void hypnotizedZombieFightsHostileZombiesInItsTile() {
        Zombie ally = zombieFactory.create("ZombieDefault", 3);
        ally.spawn(world, 5, 3);
        HypnosisService.hypnotize(ally, game.getCurrentTick());

        Zombie enemy = zombieFactory.create("ZombieDefault", 3);
        enemy.spawn(world, 5, 3);
        enemy.applyFreeze(game.getCurrentTick(), 4000);

        double enemyHealth = enemy.getHealth();

        game.advance(Game.TICKS_PER_SECOND + 1);

        assertTrue(enemy.getHealth() < enemyHealth);
    }

    private int firstElectricVictimIndex() {
        Game localGame = new Game();
        World localWorld = newWorld(101);
        localWorld.setZombieCreator(id -> zombieFactory.create(id, 3));
        localGame.register(localWorld.board());

        Plant blueberry = plantFactory.create("Electric Blueberry");
        localWorld.board().plant(6, 3, blueberry);
        blueberry.place(localWorld, 6, 3, 0);
        localGame.register(blueberry);

        List<Zombie> zombies = List.of(
                spawnFrozen(localWorld, localGame, 2, 1),
                spawnFrozen(localWorld, localGame, 2, 3),
                spawnFrozen(localWorld, localGame, 2, 5)
        );

        localGame.advance(FIRST_SHOT_TICKS + 30);

        for (int index = 0; index < zombies.size(); index++) {
            if (zombies.get(index).isDead()) {
                return index;
            }
        }

        return -1;
    }

    private Zombie spawnFrozen(
            World targetWorld,
            Game targetGame,
            int column,
            int row
    ) {
        Zombie zombie = zombieFactory.create("ZombieDefault", 3);
        zombie.spawn(targetWorld, column, row);
        zombie.applyFreeze(targetGame.getCurrentTick(), 4000);
        return zombie;
    }

    private World newWorld(long seed) {
        return new World(
                game,
                new Board(9, 5),
                new BattleResources(1000, 0),
                new Random(seed)
        );
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
        zombie.applyFreeze(game.getCurrentTick(), 4000);
        return zombie;
    }
}
