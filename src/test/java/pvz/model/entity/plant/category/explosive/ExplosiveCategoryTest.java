package pvz.model.entity.plant.category.explosive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pvz.data.PlantCsvLoader;
import pvz.data.PlantData;
import pvz.data.ZombieCsvLoader;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.Tile;
import pvz.model.core.board.TileOverlayType;
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.plantfood.PlantFoodSupport;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class ExplosiveCategoryTest {

    private static final List<String> EXPLOSIVE_PLANTS = List.of(
            "Potato Mine",
            "Primal Potato Mine",
            "Cherry Bomb",
            "Squash",
            "Grapeshot",
            "Jalapeno",
            "Doom-shroom",
            "Tangle Kelp",
            "Iceberg Lettuce",
            "Ice-shroom",
            "Hot Potato",
            "Grave Buster"
    );

    private Game game;
    private World world;
    private PlantData plantData;
    private PlantFactory plantFactory;
    private ZombieFactory zombieFactory;

    @BeforeEach
    void setUp() throws IOException {
        game = new Game();
        world = new World(
                game,
                new Board(9, 5),
                new BattleResources(1000, 0),
                new Random(3)
        );
        plantData = PlantCsvLoader.load("assets/Data/plants.csv");
        plantFactory = new PlantFactory(plantData.byName());
        zombieFactory = new ZombieFactory(
                ZombieCsvLoader.load("assets/Data/zombies.csv")
        );
        world.setZombieCreator(id -> zombieFactory.create(id, 3));
        game.register(world.board());
        GameEvents.drain();
    }

    @Test
    void everyExplosivePlantExceptBombardMintHasABehavior() {
        for (String name : EXPLOSIVE_PLANTS) {
            PlantSpec spec = spec(name);

            assertEquals(PlantCategory.EXPLOSIVE, spec.getCategory());
            assertTrue(
                    ExplosiveProfiles.isSupported(spec),
                    name + " must have an explosive profile"
            );
            assertNotNull(
                    plantFactory.create(name).behaviorCapability(
                            AbstractExplosiveBehavior.class
                    ),
                    name + " must not fall back to a passive behavior"
            );
        }

        PlantSpec bombardMint = spec("Bombard-mint");
        assertEquals(PlantCategory.EXPLOSIVE, bombardMint.getCategory());
        assertFalse(ExplosiveProfiles.isSupported(bombardMint));
        assertNull(
                plantFactory.create("Bombard-mint").behaviorCapability(
                        AbstractExplosiveBehavior.class
                )
        );
    }

    @Test
    void explodeONutStillComesFromTheWallCategory() {
        PlantSpec spec = spec("Explode-o-nut");

        assertEquals(PlantCategory.WALL, spec.getCategory());
        assertNull(
                plantFactory.create("Explode-o-nut").behaviorCapability(
                        AbstractExplosiveBehavior.class
                )
        );
        assertFalse(ExplosiveProfiles.isSupported(spec));
        assertTrue(PlantFoodSupport.isImplemented(spec));
    }

    @Test
    void plantFoodSupportMatchesTheStageDecisions() {
        for (String name : List.of(
                "Potato Mine",
                "Primal Potato Mine",
                "Squash",
                "Tangle Kelp",
                "Iceberg Lettuce"
        )) {
            assertTrue(
                    PlantFoodSupport.isImplemented(spec(name)),
                    name + " must support plant food"
            );
        }

        for (String name : List.of(
                "Cherry Bomb",
                "Grapeshot",
                "Jalapeno",
                "Doom-shroom",
                "Ice-shroom",
                "Hot Potato",
                "Grave Buster",
                "Bombard-mint"
        )) {
            assertFalse(
                    PlantFoodSupport.isImplemented(spec(name)),
                    name + " must not support plant food"
            );
        }
    }

    @Test
    void explosionHitsEveryHostileContentAndSparesFriendlyContent() {
        world.board().setTileType(4, 2, TileType.TOMBSTONE);
        world.board().setTileType(5, 4, TileType.WATER);

        Plant shielded = placePlant("Peashooter", 3, 2);
        Plant pumpkin = placePlant("Pumpkin", 3, 2);
        Plant lilyPad = placePlant("Lily Pad", 5, 4);
        Plant frozenPlant = placePlant("Peashooter", 3, 4);
        Plant octopusPlant = placePlant("Peashooter", 5, 2);

        for (int level = 0; level < Plant.FULL_FREEZE_LEVEL; level++) {
            world.board().addPlantFreezeLevel(
                    frozenPlant,
                    Plant.FULL_FREEZE_LEVEL
            );
        }
        assertTrue(world.board().coverPlantWithOctopus(octopusPlant));

        Zombie zombie = spawnZombie("ZombieDefault", 5, 3);
        PushedObstacle iceBlock = spawnIceBlock(3, 3);

        double shieldedHealth = shielded.getHealth();
        double pumpkinHealth = pumpkin.getHealth();
        double lilyPadHealth = lilyPad.getHealth();
        double frozenHealth = frozenPlant.getHealth();

        placePlant("Cherry Bomb", 4, 3);
        game.advance(5);

        assertTrue(zombie.isDead());
        assertTrue(iceBlock.isDead());
        assertEquals(TileType.NORMAL, tile(4, 2).getType());
        assertFalse(tile(3, 4).hasOverlay(TileOverlayType.FROZEN));
        assertFalse(tile(5, 2).hasOverlay(TileOverlayType.OCTOPUS));

        assertEquals(shieldedHealth, shielded.getHealth());
        assertEquals(pumpkinHealth, pumpkin.getHealth());
        assertEquals(lilyPadHealth, lilyPad.getHealth());
        assertEquals(frozenHealth, frozenPlant.getHealth());
        assertEquals(TileType.WATER, tile(5, 4).getType());
        assertEquals(TileType.NORMAL, tile(3, 3).getType());
    }

    @Test
    void enemyContentTakesExplosionDamageExactlyOnce() {
        Zombie gargantuar = spawnZombie("ZombieGargantuar", 5, 3);
        PushedObstacle iceBlock = spawnIceBlock(4, 3);
        double zombieHealth = gargantuar.getHealth();
        double obstacleHealth = iceBlock.getHealth();

        world.damageEnemyContentsInArea(4, 3, 1, 10);

        assertEquals(zombieHealth - 10, gargantuar.getHealth());
        assertEquals(obstacleHealth - 10, iceBlock.getHealth());
    }


    @Test
    void explosiveAbilityDamageConsumesArmorBeforeZombieHealth() {
        Zombie bucketHead = spawnZombie("ZombieArmor2", 5, 3);
        double healthBefore = bucketHead.getHealth();
        double armorBefore = bucketHead.getArmorHealth();

        world.damageEnemyContentsInArea(5, 3, 0, 500);

        assertEquals(healthBefore, bucketHead.getHealth());
        assertEquals(armorBefore - 500, bucketHead.getArmorHealth());
    }

    @Test
    void explosiveRowDamageConsumesArmorBeforeZombieHealth() {
        Zombie bucketHead = spawnZombie("ZombieArmor2", 8, 3);
        double healthBefore = bucketHead.getHealth();
        double armorBefore = bucketHead.getArmorHealth();

        world.damageEnemyContentsInRow(3, 500);

        assertEquals(healthBefore, bucketHead.getHealth());
        assertEquals(armorBefore - 500, bucketHead.getArmorHealth());
    }

    @Test
    void explosiveLawnDamageConsumesArmorBeforeZombieHealth() {
        Zombie bucketHead = spawnZombie("ZombieArmor2", 8, 3);
        double healthBefore = bucketHead.getHealth();
        double armorBefore = bucketHead.getArmorHealth();

        world.damageAllEnemyContents(500);

        assertEquals(healthBefore, bucketHead.getHealth());
        assertEquals(armorBefore - 500, bucketHead.getArmorHealth());
    }

    @Test
    void explicitDirectDamageStillBypassesArmor() {
        Zombie bucketHead = spawnZombie("ZombieArmor2", 5, 3);
        double healthBefore = bucketHead.getHealth();
        double armorBefore = bucketHead.getArmorHealth();

        bucketHead.takeDirectDamage(50);

        assertEquals(healthBefore - 50, bucketHead.getHealth());
        assertEquals(armorBefore, bucketHead.getArmorHealth());
    }

    @Test
    void explosionDamagesEveryDestructibleOverlayOnTheTile() {
        Plant peashooter = placePlant("Peashooter", 4, 3);
        Plant pumpkin = placePlant("Pumpkin", 4, 3);

        for (int level = 0; level < Plant.FULL_FREEZE_LEVEL; level++) {
            world.board().addPlantFreezeLevel(
                    peashooter,
                    Plant.FULL_FREEZE_LEVEL
            );
            world.board().addPlantFreezeLevel(
                    pumpkin,
                    Plant.FULL_FREEZE_LEVEL
            );
        }

        assertEquals(2, tile(4, 3).getOverlays().size());
        double peashooterHealth = peashooter.getHealth();
        double pumpkinHealth = pumpkin.getHealth();

        world.damageEnemyContentsInArea(4, 3, 0, 600);

        assertTrue(tile(4, 3).getOverlays().isEmpty());
        assertEquals(0, peashooter.getFreezeLevel());
        assertEquals(0, pumpkin.getFreezeLevel());
        assertEquals(peashooterHealth, peashooter.getHealth());
        assertEquals(pumpkinHealth, pumpkin.getHealth());
    }

    @Test
    void hasEnemyContentAtSeesZombiesObstaclesAndDestructibleTerrain() {
        assertFalse(world.hasEnemyContentAt(2, 2));

        spawnZombie("ZombieDefault", 2, 2);
        assertTrue(world.hasEnemyContentAt(2, 2));

        spawnIceBlock(3, 2);
        assertTrue(world.hasEnemyContentAt(3, 2));

        world.board().setTileType(4, 2, TileType.TOMBSTONE);
        assertTrue(world.hasEnemyContentAt(4, 2));

        world.board().setTileType(5, 2, TileType.WATER);
        assertFalse(world.hasEnemyContentAt(5, 2));
    }

    @Test
    void singleUsePlantResolvesAfterFiveTickActivationAndOnlyOnce() {
        Zombie firstZombie = spawnZombie("ZombieDefault", 4, 3);
        Plant cherryBomb = placePlant("Cherry Bomb", 4, 3);

        assertFalse(firstZombie.isDead());
        assertFalse(cherryBomb.isRemovedFromWorld());
        assertTrue(cherryBomb.canBeEatenByZombie());
        assertEquals(
                TransientActionWindow.State.EFFECT_ACTIVE,
                cherryBomb.behaviorCapability(
                        AbstractExplosiveBehavior.class
                ).window().getState()
        );

        game.advance(4);

        assertFalse(firstZombie.isDead());
        assertFalse(cherryBomb.isRemovedFromWorld());
        assertTrue(tile(4, 3).getPlants().contains(cherryBomb));

        Zombie secondZombie = spawnZombie("ZombieDefault", 4, 4);

        game.advance(1);

        assertTrue(firstZombie.isDead());
        assertTrue(secondZombie.isDead());
        assertTrue(cherryBomb.isRemovedFromWorld());
        assertFalse(tile(4, 3).getPlants().contains(cherryBomb));
    }

    private PlantSpec spec(String name) {
        return plantData.byName().get(name.toLowerCase());
    }

    private Tile tile(int column, int row) {
        return world.board().getTile(column, row);
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

    private PushedObstacle spawnIceBlock(int column, int row) {
        PushedObstacle obstacle = new PushedObstacle(
                "ICE_BLOCK",
                "Ice Block",
                600,
                true,
                true,
                true
        );
        obstacle.spawn(world, column - 0.5, row - 0.5);
        game.register(obstacle);
        return obstacle;
    }
}
