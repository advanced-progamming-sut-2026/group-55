package pvz.model.session;

import java.awt.Point;
import java.util.Objects;

import pvz.model.core.Board;
import pvz.model.core.Game;
import pvz.model.core.SunBank;
import pvz.model.core.World;
import pvz.model.entity.collectible.sun.SkySunSpawner;
import pvz.model.entity.plant.PlantFactory;

public final class GameSessionFactory {
    private final PlantFactory plantFactory;

    public GameSessionFactory(PlantFactory plantFactory) {
        this.plantFactory = Objects.requireNonNull(
                plantFactory,
                "plant factory cannot be null"
        );
    }

    public GameSession create(GameSessionConfig config) {
        Objects.requireNonNull(config, "config cannot be null");

        Game game = new Game();

        Board board = new Board(config.columns(), config.rows() );

        addInitialTombstones(board, config);

        SunBank sunBank = new SunBank(config.startingSun());

        World world = new World(game, board, sunBank);

        game.register(board);

        if (config.skySunEnabled()) {
            game.register(new SkySunSpawner(world));
        }

        return new GameSession(config, world, plantFactory);
    }

    private void addInitialTombstones(Board board, GameSessionConfig config) {
        for (Point point : config.tombCoordinates()) {
            boolean placed = board.placeTombstone(point.x, point.y);

            if (!placed) {
                throw new IllegalStateException("could not place initial tombstone at ("
                                + point.x + ", " + point.y + ")"
                );
            }
        }
    }
}