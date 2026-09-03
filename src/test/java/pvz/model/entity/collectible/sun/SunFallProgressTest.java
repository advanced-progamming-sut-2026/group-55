package pvz.model.entity.collectible.sun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.junit.jupiter.api.Test;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;

class SunFallProgressTest {

    @Test
    void skySunExposesModelDrivenFallProgress() {
        Game game = new Game();
        World world = world(game);
        Sun sun = Sun.fromSky(
                world,
                SunType.NORMAL,
                4.5,
                2.5,
                SunValue.NORMALSUN.getValue()
        );

        assertEquals(0.0, sun.getFallProgress(0L), 0.0001);
        assertEquals(0.5, sun.getFallProgress(25L), 0.0001);
        assertEquals(1.0, sun.getFallProgress(50L), 0.0001);
        assertEquals(1.0, sun.getFallProgress(500L), 0.0001);
    }

    @Test
    void fallProgressUsesActualSpawnTickAndClampsEarlyTicks() {
        Game game = new Game();
        game.advance(17L);
        World world = world(game);
        Sun sun = Sun.fromSky(
                world,
                SunType.SPECIAL,
                1.5,
                1.5,
                SunValue.SPECIALSUN.getValue()
        );

        assertEquals(17L, sun.getSpawnTick());
        assertEquals(67L, sun.getLandingTick());
        assertEquals(0.0, sun.getFallProgress(10L), 0.0001);
        assertEquals(0.5, sun.getFallProgress(42L), 0.0001);
    }

    @Test
    void nonSkySunIsAlreadyAtItsPresentationTarget() {
        Game game = new Game();
        World world = world(game);
        Sun sun = Sun.recovered(
                world,
                3.5,
                1.5,
                SunValue.BIGSUN.getValue()
        );

        assertEquals(1.0, sun.getFallProgress(game.getCurrentTick()), 0.0001);
    }

    private World world(Game game) {
        return new World(
                game,
                new Board(9, 5),
                new BattleResources(0, 0),
                new Random(1)
        );
    }
}
