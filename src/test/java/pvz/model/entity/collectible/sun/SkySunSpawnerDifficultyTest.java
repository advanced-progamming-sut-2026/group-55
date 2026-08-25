package pvz.model.entity.collectible.sun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.junit.jupiter.api.Test;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;

class SkySunSpawnerDifficultyTest {

    @Test
    void difficultyChangesSkySunIntervalWithoutChangingGameSpeed() {
        assertFirstDropTick(1, 40);
        assertFirstDropTick(3, 120);
        assertFirstDropTick(5, 200);
    }

    private void assertFirstDropTick(
            int difficultyLevel,
            int expectedDropTick
    ) {
        Game game = new Game();
        World world = new World(
                game,
                new Board(9, 5),
                new BattleResources(0, 0),
                new Random(1)
        );
        game.register(new SkySunSpawner(world, difficultyLevel));

        game.advance(expectedDropTick - 1);
        assertEquals(0, world.getCollectibles().size());

        game.advance(1);
        assertEquals(1, world.getCollectibles().size());
        assertEquals(expectedDropTick, game.getCurrentTick());
    }
}
