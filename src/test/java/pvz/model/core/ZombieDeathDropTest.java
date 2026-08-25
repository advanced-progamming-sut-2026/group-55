package pvz.model.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Random;
import org.junit.jupiter.api.Test;
import pvz.data.ZombieCsvLoader;
import pvz.model.core.board.Board;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class ZombieDeathDropTest {
    @Test
    void glowingZombieAddsPlantFoodAndCoinReward() throws IOException {
        BattleResources resources = new BattleResources(50, 0);
        World world = new World(
                new Game(),
                new Board(9, 5),
                resources,
                new FixedRandom(0)
        );
        Zombie zombie = zombieFactory().create("default", 3);
        zombie.spawn(world, 5, 1);

        assertTrue(zombie.isGlowing());
        zombie.takeDirectDamage(Double.MAX_VALUE);

        assertEquals(1, resources.getPlantFoodCount());
        assertEquals(50, resources.battleWallet().getCollectedCoins());
        assertEquals(0, resources.battleWallet().getCollectedDiamonds());
    }

    @Test
    void potRewardIsKeptUntilSessionSettlement() throws IOException {
        BattleResources resources = new BattleResources(50, 0);
        World world = new World(
                new Game(),
                new Board(9, 5),
                resources,
                new FixedRandom(2)
        );
        Zombie zombie = zombieFactory().create("default", 3);
        zombie.spawn(world, 5, 1);
        zombie.takeDirectDamage(Double.MAX_VALUE);

        assertEquals(1, resources.getCollectedPotCount());
    }

    private ZombieFactory zombieFactory() throws IOException {
        return new ZombieFactory(
                ZombieCsvLoader.load("assets/Data/zombies.csv")
        );
    }

    private static final class FixedRandom extends Random {
        private final int rewardType;

        private FixedRandom(int rewardType) {
            this.rewardType = rewardType;
        }

        @Override
        public double nextDouble() {
            return 0;
        }

        @Override
        public int nextInt(int bound) {
            return rewardType;
        }
    }
}
