package pvz.model.wave;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pvz.model.core.GameEvents;
import pvz.model.core.Updatable;
import pvz.model.core.World;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;

public final class WaveManager implements Updatable {
    private static final double NEXT_WAVE_REMAINING_RATIO = 0.25;
    private static final double SPAWNING_PROGRESS_SHARE = 0.25;

    private final World world;
    private final ZombieFactory zombieFactory;
    private final List<Wave> waves;
    private final int difficultyLevel;
    private final List<Zombie> currentWaveZombies = new ArrayList<>();

    private WaveState state = WaveState.NOT_STARTED;
    private int currentWaveIndex = -1;
    private int nextZombieIndex;
    private long nextActionTick;
    private double initialWaveVitality;
    private boolean finalWaveFullySpawned;

    public WaveManager(
            World world,
            ZombieFactory zombieFactory,
            List<Wave> waves,
            int difficultyLevel
    ) {
        this.world = Objects.requireNonNull(world, "world cannot be null");
        this.zombieFactory = Objects.requireNonNull(
                zombieFactory,
                "zombie factory cannot be null"
        );
        this.waves = List.copyOf(
                Objects.requireNonNull(waves, "waves cannot be null")
        );
        if (waves.isEmpty()) {
            throw new IllegalArgumentException("waves cannot be empty");
        }
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            throw new IllegalArgumentException(
                    "difficulty level must be between 1 and 5"
            );
        }
        this.difficultyLevel = difficultyLevel;
    }

    public void start(long currentTick) {
        if (state != WaveState.NOT_STARTED) {
            throw new IllegalStateException(
                    "wave manager has already started"
            );
        }
        state = WaveState.WAITING;
        nextActionTick = currentTick + waves.get(0).getStartDelayTicks();
    }

    @Override
    public void update(long tick) {
        if (state == WaveState.NOT_STARTED || state == WaveState.COMPLETED) {
            return;
        }
        if (finalWaveFullySpawned && world.getHostileZombies().isEmpty()) {
            state = WaveState.COMPLETED;
            return;
        }
        if (state == WaveState.WAITING && tick >= nextActionTick) {
            beginNextWave(tick);
        }
        if (state == WaveState.SPAWNING) {
            spawnDueZombies(tick);
        }
        if (state == WaveState.FIGHTING && thresholdReached()) {
            scheduleNextWave(tick);
        }
    }

    private void beginNextWave(long tick) {
        currentWaveIndex++;
        Wave wave = waves.get(currentWaveIndex);
        currentWaveZombies.clear();
        initialWaveVitality = 0;
        nextZombieIndex = 0;
        nextActionTick = tick;
        state = WaveState.SPAWNING;

        if (wave.isFinalWave()) {
            GameEvents.publish("The final wave has come.");
        } else {
            GameEvents.publish("Wave " + wave.getNumber() + " started.");
        }
    }

    private void spawnDueZombies(long tick) {
        Wave wave = waves.get(currentWaveIndex);
        while (nextZombieIndex < wave.getZombies().size()
                && tick >= nextActionTick) {
            spawnZombie(wave);
            nextActionTick += wave.getSpawnIntervalTicks();
        }

        if (nextZombieIndex == wave.getZombies().size()) {
            state = WaveState.FIGHTING;
            if (wave.isFinalWave()) {
                finalWaveFullySpawned = true;
            }
        }
    }

    private void spawnZombie(Wave wave) {
        WaveZombieEntry entry = wave.getZombies().get(nextZombieIndex);
        Zombie zombie = zombieFactory.create(
                entry.zombieType(),
                difficultyLevel
        );
        if (zombie == null) {
            throw new IllegalStateException(
                    "wave references unknown zombie: " + entry.zombieType()
            );
        }
        zombie.spawn(world, world.board().getCols(), entry.lane());
        currentWaveZombies.add(zombie);
        initialWaveVitality += vitalityOf(zombie);
        nextZombieIndex++;
        GameEvents.publish(
                "Zombie " + zombie.getName()
                        + " spawned at wave " + wave.getNumber()
                        + " in lane " + entry.lane()
                        + " which costed " + entry.cost() + "."
        );
    }

    private boolean thresholdReached() {
        Wave wave = waves.get(currentWaveIndex);
        if (wave.isFinalWave() || initialWaveVitality <= 0) {
            return false;
        }
        return remainingWaveVitality() / initialWaveVitality
                <= NEXT_WAVE_REMAINING_RATIO;
    }

    private void scheduleNextWave(long tick) {
        Wave nextWave = waves.get(currentWaveIndex + 1);
        state = WaveState.WAITING;
        nextActionTick = tick + nextWave.getStartDelayTicks();
    }

    private double remainingWaveVitality() {
        List<Zombie> activeZombies = world.getHostileZombies();
        return currentWaveZombies.stream()
                .filter(activeZombies::contains)
                .mapToDouble(this::vitalityOf)
                .sum();
    }

    private double vitalityOf(Zombie zombie) {
        return zombie.getHealth() + zombie.getArmorHealth();
    }

    public boolean isCompleted() {
        return state == WaveState.COMPLETED
                || (finalWaveFullySpawned && world.getHostileZombies().isEmpty());
    }

    public int getCurrentWaveNumber() {
        return currentWaveIndex + 1;
    }

    public int getTotalWaves() {
        return waves.size();
    }

    public WaveState getState() {
        if (isCompleted()) {
            return WaveState.COMPLETED;
        }
        return state;
    }

    /** Stable read model for progress bars and pre-wave announcements. */
    public WaveProgressSnapshot progressSnapshot() {
        WaveState visibleState = getState();
        if (visibleState == WaveState.COMPLETED) {
            return completedSnapshot();
        }
        if (visibleState == WaveState.WAITING) {
            return waitingSnapshot();
        }
        if (visibleState == WaveState.NOT_STARTED) {
            return notStartedSnapshot();
        }
        return activeWaveSnapshot(visibleState);
    }

    private WaveProgressSnapshot notStartedSnapshot() {
        return new WaveProgressSnapshot(
                0, waves.size(), WaveState.NOT_STARTED,
                0, 0, 0d, 0d, 1, 0L, waves.get(0).isFinalWave()
        );
    }

    private WaveProgressSnapshot completedSnapshot() {
        Wave finalWave = waves.get(waves.size() - 1);
        int totalZombies = finalWave.getZombies().size();
        return new WaveProgressSnapshot(
                waves.size(), waves.size(), WaveState.COMPLETED,
                totalZombies, totalZombies, 1d, 1d, 0, 0L, false
        );
    }

    private WaveProgressSnapshot waitingSnapshot() {
        int nextWaveIndex = currentWaveIndex + 1;
        int nextWaveNumber = nextWaveIndex + 1;
        long ticksRemaining = Math.max(
                0L,
                nextActionTick - world.game().getCurrentTick()
        );
        double overall = currentWaveIndex < 0
                ? 0d
                : clamp01((double) (currentWaveIndex + 1) / waves.size());
        double currentProgress = currentWaveIndex < 0 ? 0d : 1d;
        boolean nextIsFinal = waves.get(nextWaveIndex).isFinalWave();
        return new WaveProgressSnapshot(
                getCurrentWaveNumber(), waves.size(), WaveState.WAITING,
                currentWaveIndex < 0 ? 0 : currentWaveZombies.size(),
                currentWaveIndex < 0 ? 0 : currentWaveZombies.size(),
                currentProgress, overall, nextWaveNumber, ticksRemaining,
                nextIsFinal
        );
    }

    private WaveProgressSnapshot activeWaveSnapshot(WaveState visibleState) {
        Wave wave = waves.get(currentWaveIndex);
        int totalZombies = wave.getZombies().size();
        double waveProgress = visibleState == WaveState.SPAWNING
                ? spawningProgress(totalZombies)
                : fightingProgress(wave.isFinalWave());
        double overall = clamp01(
                (currentWaveIndex + waveProgress) / waves.size()
        );
        return new WaveProgressSnapshot(
                wave.getNumber(), waves.size(), visibleState,
                nextZombieIndex, totalZombies, waveProgress, overall,
                0, 0L, false
        );
    }

    private double spawningProgress(int totalZombies) {
        if (totalZombies <= 0) {
            return 0d;
        }
        return clamp01(
                SPAWNING_PROGRESS_SHARE
                        * ((double) nextZombieIndex / totalZombies)
        );
    }

    private double fightingProgress(boolean finalWave) {
        if (initialWaveVitality <= 0d) {
            return SPAWNING_PROGRESS_SHARE;
        }
        double remainingRatio = clamp01(
                remainingWaveVitality() / initialWaveVitality
        );
        double lostRatio = 1d - remainingRatio;
        double goalRatio = finalWave
                ? 1d
                : 1d - NEXT_WAVE_REMAINING_RATIO;
        double normalizedLoss = clamp01(lostRatio / goalRatio);
        return clamp01(
                SPAWNING_PROGRESS_SHARE
                        + (1d - SPAWNING_PROGRESS_SHARE) * normalizedLoss
        );
    }

    private static double clamp01(double value) {
        return Math.max(0d, Math.min(1d, value));
    }

    public enum WaveState {
        NOT_STARTED,
        WAITING,
        SPAWNING,
        FIGHTING,
        COMPLETED
    }
}
