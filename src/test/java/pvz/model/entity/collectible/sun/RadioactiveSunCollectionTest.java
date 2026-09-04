package pvz.model.entity.collectible.sun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.core.board.TileType;

class RadioactiveSunCollectionTest {

    @Test
    void fallingRadioactiveSunExplodesAtCollectionTile() {
        Game game = new Game();
        Board board = new Board(9, 5);
        World world = world(game, board);
        board.setTileType(2, 2, TileType.TOMBSTONE);
        board.setTileType(8, 5, TileType.TOMBSTONE);

        Sun sun = Sun.fromSky(
                world,
                SunType.RADIOACTIVE,
                7.5,
                4.5,
                50
        );
        world.addCollectible(sun);
        game.register(sun);

        SunCollectionOutcome outcome = world.collectSun(sun, 2, 2);

        assertEquals(SunCollectionOutcome.EXPLODED, outcome);
        assertEquals(550d, board.getTile(2, 2).getHealth(), 0.0001d);
        assertEquals(700d, board.getTile(8, 5).getHealth(), 0.0001d);
        assertTrue(sun.isRemoved());
    }


    @Test
    void collectionWithoutExplicitTileKeepsLandingTileBehavior() {
        Game game = new Game();
        Board board = new Board(9, 5);
        World world = world(game, board);
        board.setTileType(8, 5, TileType.TOMBSTONE);

        Sun sun = Sun.fromSky(
                world,
                SunType.RADIOACTIVE,
                7.5,
                4.5,
                50
        );
        world.addCollectible(sun);
        game.register(sun);

        SunCollectionOutcome outcome = world.collectSun(sun);

        assertEquals(SunCollectionOutcome.EXPLODED, outcome);
        assertEquals(550d, board.getTile(8, 5).getHealth(), 0.0001d);
    }

    @Test
    void collectionTileMustBeInsideBoard() {
        Game game = new Game();
        World world = world(game, new Board(9, 5));
        Sun sun = Sun.fromSky(
                world,
                SunType.RADIOACTIVE,
                4.5,
                2.5,
                50
        );
        world.addCollectible(sun);
        game.register(sun);

        assertThrows(
                IllegalArgumentException.class,
                () -> world.collectSun(sun, 0, 3)
        );
    }

    private World world(Game game, Board board) {
        return new World(
                game,
                board,
                new BattleResources(0, 0),
                new Random(1)
        );
    }
}
