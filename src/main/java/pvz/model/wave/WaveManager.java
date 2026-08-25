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
        if (finalWaveFullySpawned && world.getZombies().isEmpty()) {
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
            WaveZombieEntry entry = wave.getZombies().get(nextZombieIndex);
            Zombie zombie = zombieFactory.create(
                    entry.zombieType(),
                    difficultyLevel
            );
            if (zombie == null) {
                throw new IllegalStateException(
                        "wave references unknown zombie: "
                                + entry.zombieType()
                );
            }
            zombie.spawn(
                    world,
                    world.board().getCols(),
                    entry.lane()
            );
            currentWaveZombies.add(zombie);
            initialWaveVitality += vitalityOf(zombie);
            nextZombieIndex++;
            nextActionTick += wave.getSpawnIntervalTicks();
            GameEvents.publish(
                    "Zombie " + zombie.getName()
                            + " spawned at wave " + wave.getNumber()
                            + " in lane " + entry.lane()
                            + " which costed " + entry.cost() + "."
            );
        }

        if (nextZombieIndex == wave.getZombies().size()) {
            state = WaveState.FIGHTING;
            if (wave.isFinalWave()) {
                finalWaveFullySpawned = true;
            }
        }
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
        List<Zombie> activeZombies = world.getZombies();
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
                || (finalWaveFullySpawned && world.getZombies().isEmpty());
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

    public enum WaveState {
        NOT_STARTED,
        WAITING,
        SPAWNING,
        FIGHTING,
        COMPLETED
    }
}
