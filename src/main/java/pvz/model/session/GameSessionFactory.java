package pvz.model.session;

import java.awt.Point;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.random.RandomGenerator;

import pvz.model.core.BattleResources;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.entity.collectible.sun.SkySunSpawner;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.ZombieFactory;
import pvz.model.wave.Wave;
import pvz.model.wave.WaveGenerator;
import pvz.model.wave.WaveManager;

public final class GameSessionFactory {

    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final RandomGenerator random;


    public GameSessionFactory(
            PlantFactory plantFactory,
            ZombieFactory zombieFactory
    ) {
        this(plantFactory, zombieFactory, new Random());
    }

    GameSessionFactory(
            PlantFactory plantFactory,
            ZombieFactory zombieFactory,
            RandomGenerator random
    ) {
        this.plantFactory = Objects.requireNonNull(
                plantFactory,
                "plant factory cannot be null"
        );

        this.zombieFactory = Objects.requireNonNull(
                zombieFactory,
                "zombie factory cannot be null"
        );
        this.random = Objects.requireNonNull(
                random,
                "random generator cannot be null"
        );
    }


    public GameSession create(GameSessionConfig config) {
        Objects.requireNonNull(config, "config cannot be null");

        Game game = new Game();

        Board board = new Board(
                config.columns(),
                config.rows()
        );

        addInitialTombstones(board, config);

        BattleResources resources =
                new BattleResources(config.startingSun(), config.startingPlantFood());

        World world =
                new World(game, board, resources, random);


        game.register(board);


        if (config.skySunEnabled()) {
            game.register(new SkySunSpawner(world));
        }

        List<Wave> waves = new WaveGenerator(
                zombieFactory,
                random
        ).generate(
                config.waveConfiguration(),
                config.rows(),
                config.difficultyLevel()
        );
        WaveManager waveManager = new WaveManager(
                world,
                zombieFactory,
                waves,
                config.difficultyLevel()
        );
        game.register(waveManager);


        return new GameSession(
                config,
                world,
                plantFactory,
                zombieFactory,
                waveManager
        );
    }


    private void addInitialTombstones(
            Board board,
            GameSessionConfig config
    ) {
        for (Point point : config.tombCoordinates()) {

            boolean placed = board.placeTombstone(
                    point.x,
                    point.y
            );

            if (!placed) {
                throw new IllegalStateException(
                        "could not place initial tombstone at ("
                                + point.x
                                + ", "
                                + point.y
                                + ")"
                );
            }
        }
    }
}
