package pvz.model.entity.zombie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pvz.data.ZombieCsvLoader;
import pvz.data.ZombieData;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.behavior.ZombieBehaviorFactory;

class ZombieBehaviorIntegrationTest {
    @Test
    void allFinalDataZombiesCanBeCreated() throws IOException {
        ZombieData data = ZombieCsvLoader.load("assets/Data/zombies.csv");
        ZombieFactory factory = new ZombieFactory(data);

        assertEquals(28, data.byId().size());
        for (ZombieSpec spec : data.byId().values()) {
            assertNotNull(factory.create(spec.getId(), 3));
        }
    }

    @Test
    void projectileDefensesUseAttackContext() throws IOException {
        ZombieFactory factory = factory();
        World world = world(factory);

        Zombie dragon = factory.create("ZombieDarkImpDragon", 3);
        dragon.spawn(world, 9, 1);
        double dragonHealth = dragon.getHealth();
        ProjectileType.FIRE.hitZombie(dragon, 100, 0);
        assertEquals(dragonHealth, dragon.getHealth());

        Zombie umbrella = factory.create("ZombieLostCityJane", 3);
        umbrella.spawn(world, 9, 2);
        double umbrellaHealth = umbrella.getHealth();
        ProjectileType.NORMAL.hitZombie(
                umbrella,
                100,
                0,
                DamageContext.AttackDelivery.LOBBED
        );
        assertEquals(umbrellaHealth, umbrella.getHealth());
    }

    @Test
    void gargantuarThrowsOneImpAtHalfHealth() throws IOException {
        ZombieFactory factory = factory();
        World world = world(factory);
        Zombie gargantuar = factory.create("ZombieGargantuar", 3);
        gargantuar.spawn(world, 9, 3);

        gargantuar.takeDirectDamage(gargantuar.getMaximumHealth() / 2.0);
        gargantuar.update(1);
        gargantuar.update(2);

        long impCount = world.getZombies().stream()
                .filter(zombie -> zombie.getSpec().getId().equals("ZombieImp"))
                .count();
        assertEquals(1L, impCount);
    }

    @Test
    void behaviorFactoryRejectsCsvParametersThatCodeDoesNotUse() {
        ZombieBehaviorDefinition definition = new ZombieBehaviorDefinition(
                "TURQUOISE_LASER",
                Map.ofEntries(
                        Map.entry("detectionRadiusTiles", "4"),
                        Map.entry("laserRangeTiles", "4"),
                        Map.entry("sunPerSecond", "25"),
                        Map.entry("chargingSeconds", "5"),
                        Map.entry("cooldownSeconds", "5"),
                        Map.entry("sunDropRatio", "0.5"),
                        Map.entry("obsoleteRange", "4")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ZombieBehaviorFactory().create(definition)
        );
    }

    private ZombieFactory factory() throws IOException {
        return new ZombieFactory(
                ZombieCsvLoader.load("assets/Data/zombies.csv")
        );
    }

    private World world(ZombieFactory factory) {
        World world = new World(
                new Game(),
                new Board(9, 5),
                new BattleResources(500, 0)
        );
        world.setZombieCreator(id -> factory.create(id, 3));
        return world;
    }
}
