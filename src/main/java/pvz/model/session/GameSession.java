package pvz.model.session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import pvz.model.core.BattleResources;
import pvz.model.core.BattleWallet;
import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.core.GameStatus;
import pvz.model.core.World;
import pvz.model.core.board.Board;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;
import pvz.model.quest.QuestEvent;
import pvz.model.quest.QuestEventBuffer;
import pvz.model.session.condition.WinConditionContext;
import pvz.model.wave.WaveManager;

public final class GameSession {
    private final GameSessionConfig config;
    private final Game game;
    private final Board board;
    private final World world;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final WaveManager waveManager;
    private final QuestEventBuffer questEvents;
    private final StoredPlantBoostAccess storedBoostAccess;
    private final Set<String> pendingStoredBoosts;
    private final Set<String> activatedStoredBoosts = new HashSet<>();
    private final Map<String, Long> lastPlantedTicks = new HashMap<>();

    private GameSessionStatus status = GameSessionStatus.CREATED;

    GameSession(
            GameSessionConfig config,
            World world,
            PlantFactory plantFactory,
            ZombieFactory zombieFactory,
            WaveManager waveManager,
            QuestEventBuffer questEvents,
            StoredPlantBoostAccess storedBoostAccess
    ) {
        this.config = Objects.requireNonNull(
                config,
                "config cannot be null"
        );
        this.world = Objects.requireNonNull(
                world,
                "world cannot be null"
        );
        this.plantFactory = Objects.requireNonNull(
                plantFactory,
                "plant factory cannot be null"
        );
        this.zombieFactory = Objects.requireNonNull(
                zombieFactory,
                "zombie factory cannot be null"
        );
        this.waveManager = Objects.requireNonNull(
                waveManager,
                "wave manager cannot be null"
        );
        this.questEvents = Objects.requireNonNull(
                questEvents,
                "quest event buffer cannot be null"
        );
        this.storedBoostAccess = Objects.requireNonNull(
                storedBoostAccess,
                "stored boost access cannot be null"
        );
        this.pendingStoredBoosts = new HashSet<>(config.storedBoostPlants());
        this.game = world.game();
        this.board = world.board();
    }

    public void start() {
        if (status != GameSessionStatus.CREATED) {
            throw new IllegalStateException(
                    "session has already started"
            );
        }

        status = GameSessionStatus.RUNNING;
        waveManager.start(game.getCurrentTick());
    }

    public void advance(long ticks) {
        requireRunning();
        game.advance(ticks);
        checkGameState();
    }

    public Plant createPlant(String plantName) {
        requireRunning();
        String normalizedName = normalizeName(plantName);
        int level = config.plantLevels().getOrDefault(normalizedName, 1);
        return plantFactory.create(normalizedName, level);
    }

    public Zombie createZombie(String zombieName) {
        requireRunning();
        return zombieFactory.create(
                normalizeName(zombieName),
                config.difficultyLevel()
        );
    }

    public boolean isPlantSelected(String plantName) {
        return config.selectedPlants().contains(
                normalizeName(plantName)
        );
    }

    public boolean isPlantBoosted(String plantName) {
        String normalizedName = normalizeName(plantName);
        return isPlantManuallyBoosted(normalizedName)
                || isStoredBoostActivated(normalizedName)
                || hasPendingStoredBoost(normalizedName);
    }

    public boolean isPlantManuallyBoosted(String plantName) {
        return config.boostedPlants().contains(
                normalizeName(plantName)
        );
    }

    public boolean hasPendingStoredBoost(String plantName) {
        String normalizedName = normalizeName(plantName);
        return pendingStoredBoosts.contains(normalizedName)
                && storedBoostAccess.isAvailable(normalizedName);
    }

    public boolean isStoredBoostActivated(String plantName) {
        return activatedStoredBoosts.contains(normalizeName(plantName));
    }

    /**
     * Consumes a greenhouse boost only after a successful planting operation
     * and activates the ordinary stage-wide boost for the rest of this run.
     * The persistent boundary is asked first; on failure the session keeps the
     * reward pending so a later planting can retry without losing it.
     */
    public boolean activateStoredBoost(String plantName) {
        requireRunning();
        String normalizedName = normalizeName(plantName);
        if (!hasPendingStoredBoost(normalizedName)) {
            return false;
        }
        if (!storedBoostAccess.consume(normalizedName)) {
            return false;
        }
        pendingStoredBoosts.remove(normalizedName);
        activatedStoredBoosts.add(normalizedName);
        return true;
    }

    public Set<String> pendingStoredBoostsSnapshot() {
        return pendingStoredBoosts.stream()
                .filter(storedBoostAccess::isAvailable)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public Set<String> activatedStoredBoostsSnapshot() {
        return Set.copyOf(activatedStoredBoosts);
    }

    public long getRemainingRechargeTicks(
            String plantName,
            long rechargeTicks
    ) {
        if (rechargeTicks < 0) {
            throw new IllegalArgumentException(
                    "recharge ticks cannot be negative"
            );
        }

        if (resources().isCooldownCheatEnabled()) {
            return 0;
        }

        Long lastTick = lastPlantedTicks.get(
                normalizeName(plantName)
        );

        if (lastTick == null) {
            return 0;
        }

        long elapsedTicks = game.getCurrentTick() - lastTick;
        return Math.max(0, rechargeTicks - elapsedTicks);
    }

    public void recordPlanting(String plantName) {
        requireRunning();
        lastPlantedTicks.put(
                normalizeName(plantName),
                game.getCurrentTick()
        );
    }

    /**
     * Records a successful player planting operation for cooldown and quest
     * telemetry. Failed placement attempts must never call this overload.
     */
    public void recordPlanting(
            String plantName,
            int sunCost
    ) {
        if (sunCost < 0) {
            throw new IllegalArgumentException(
                    "plant sun cost cannot be negative"
            );
        }

        recordPlanting(plantName);
        questEvents.publish(QuestEvent.plantPlaced(plantName));
        if (sunCost > 0) {
            questEvents.publish(QuestEvent.sunSpent(sunCost));
        }
    }

    public void publishQuestEvent(QuestEvent event) {
        questEvents.publish(event);
    }

    public java.util.List<QuestEvent> questEventsSnapshot() {
        return questEvents.snapshot();
    }

    public java.util.List<QuestEvent> drainQuestEvents() {
        return questEvents.drain();
    }

    public void clearQuestEvents() {
        questEvents.clear();
    }


    public void resetFamilyRecharge(pvz.model.entity.plant.PlantCategory category) {
        requireRunning();
        Objects.requireNonNull(category, "plant category cannot be null");
        lastPlantedTicks.keySet().removeIf(plantName -> {
            pvz.model.entity.plant.PlantSpec spec = plantFactory.getSpec(plantName);
            return spec != null
                    && spec.getCategory() == category
                    && !spec.getTags().contains(pvz.model.entity.plant.PlantTag.MINT);
        });
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
        Objects.requireNonNull(
                finalStatus,
                "final status cannot be null"
        );

        if (isFinished()) {
            return;
        }

        requireRunning();
        status = finalStatus;
    }

    private void checkGameState() {
        GameStatus gameStatus = game.getStateManager().getStatus();

        if (gameStatus == GameStatus.LOST) {
            markLost();
            GameEvents.publish(
                    "The zombie ate your brain; LOSER!!!"
            );
            return;
        }

        if (gameStatus == GameStatus.PLAYING
                && config.winCondition().isSatisfied(
                        new WinConditionContext(
                                world,
                                waveManager,
                                game.getCurrentTick()
                        )
                )) {
            game.getStateManager().win();
            gameStatus = GameStatus.WON;
        }

        if (gameStatus == GameStatus.WON) {
            markWon();
            GameEvents.publish(
                    "Dear humanz, zis is not done yet; "
                            + "we will come back to eat your brainz, humanz."
            );
        }
    }

    private void requireRunning() {
        if (!isRunning()) {
            throw new IllegalStateException(
                    "game session is not running"
            );
        }
    }

    private String normalizeName(String name) {
        Objects.requireNonNull(name, "name cannot be null");

        String normalizedName = name.strip()
                .toLowerCase(Locale.ROOT);

        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException(
                    "name cannot be blank"
            );
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

    public WaveManager waveManager() {
        return waveManager;
    }
}
