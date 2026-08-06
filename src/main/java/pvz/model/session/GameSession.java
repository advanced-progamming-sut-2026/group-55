package pvz.model.session;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import pvz.model.core.board.Board;
import pvz.model.core.Game;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;
import pvz.model.core.BattleResources;
import pvz.model.core.BattleWallet;

public final class GameSession {
    private final GameSessionConfig config;
    private final Game game;
    private final Board board;
    private final World world;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final Map<String, Long> lastPlantedTicks = new HashMap<>();

    private GameSessionStatus status = GameSessionStatus.CREATED;

    GameSession(
            GameSessionConfig config,
            World world,
            PlantFactory plantFactory,
            ZombieFactory zombieFactory
    ) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.world = Objects.requireNonNull(world, "world cannot be null");
        this.plantFactory = Objects.requireNonNull(
                plantFactory,
                "plant factory cannot be null"
        );
        this.zombieFactory = Objects.requireNonNull(
                zombieFactory,
                "zombie factory cannot be null"
        );
        this.game = world.game();
        this.board = world.board();
    }

    public void start() {
        if (status != GameSessionStatus.CREATED) {
            throw new IllegalStateException("session has already started");
        }
        status = GameSessionStatus.RUNNING;
    }

    public void advance(long ticks) {
        requireRunning();
        game.advance(ticks);
    }

    public Plant createPlant(String plantName) {
        requireRunning();
        return plantFactory.create(normalizeName(plantName));
    }

    public Zombie createZombie(String zombieName) {
        requireRunning();
        return zombieFactory.create(normalizeName(zombieName));
    }

    public boolean isPlantSelected(String plantName) {
        return config.selectedPlants().contains(normalizeName(plantName));
    }

    public boolean isPlantBoosted(String plantName) {
        return config.boostedPlants().contains(normalizeName(plantName));
    }

    public long getRemainingRechargeTicks(String plantName, long rechargeTicks) {
        if (rechargeTicks < 0) {
            throw new IllegalArgumentException("recharge ticks cannot be negative");
        }

        if (resources().isCooldownCheatEnabled()) {
            return 0;
        }

        Long lastTick = lastPlantedTicks.get(normalizeName(plantName));

        if (lastTick == null) {
            return 0;
        }

        long elapsedTicks = game.getCurrentTick() - lastTick;

        return Math.max(0, rechargeTicks - elapsedTicks);
    }

    public void recordPlanting(String plantName) {
        requireRunning();
        lastPlantedTicks.put(normalizeName(plantName), game.getCurrentTick());
    }

    public BattleResources resources() {
        return world.resources();
    }

    public BattleWallet battleWallet() {
        return resources().battleWallet();
    }

    public void markWon() {
        finish(GameSessionStatus.WON);
    }

    public void markLost() {
        finish(GameSessionStatus.LOST);
    }

    public void abort() {
        finish(GameSessionStatus.ABORTED);
    }

    private void finish(GameSessionStatus finalStatus) {
        requireRunning();
        status = finalStatus;
    }

    private void requireRunning() {
        if (!isRunning()) {
            throw new IllegalStateException("game session is not running");
        }
    }

    private String normalizeName(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        String normalizedName = name.strip().toLowerCase(Locale.ROOT);

        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("name cannot be blank");
        }

        return normalizedName;
    }

    public boolean isRunning() {
        return status == GameSessionStatus.RUNNING;
    }

    public boolean isFinished() {
        return status == GameSessionStatus.WON
                || status == GameSessionStatus.LOST
                || status == GameSessionStatus.ABORTED;
    }

    public GameSessionConfig config() {
        return config;
    }

    public Game game() {
        return game;
    }

    public Board board() {
        return board;
    }

    public World world() {
        return world;
    }

    public GameSessionStatus status() {
        return status;
    }
}
