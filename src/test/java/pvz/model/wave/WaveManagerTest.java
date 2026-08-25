package pvz.model.wave;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import pvz.data.ZombieCsvLoader;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

class WaveManagerTest {
    @Test
    void startsNextWaveAfterSeventyFivePercentVitalityIsLost()
            throws IOException {
        ZombieFactory zombieFactory = new ZombieFactory(
                ZombieCsvLoader.load("assets/Data/zombies.csv")
        );
        Game game = new Game();
        Board board = new Board(9, 5);
        World world = new World(
                game,
                board,
                new BattleResources(50, 0)
        );
        Wave first = new Wave(
                1,
                List.of(new WaveZombieEntry("ZombieDefault", 1, 100)),
                0,
                0,
                false
        );
        Wave second = new Wave(
                2,
                List.of(new WaveZombieEntry("ZombieDefault", 2, 100)),
                0,
                0,
                true
        );
        WaveManager manager = new WaveManager(
                world,
                zombieFactory,
                List.of(first, second),
                3
        );
        game.register(board);
        game.register(manager);
        manager.start(0);

        game.advance(1);
        Zombie firstZombie = world.getZombies().get(0);
        firstZombie.takeDirectDamage(142.5);
        game.advance(2);

        assertEquals(2, manager.getCurrentWaveNumber());
        assertEquals(2, world.getZombies().size());
    }
}
