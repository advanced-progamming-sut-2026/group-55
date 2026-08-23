package pvz.model.wave;

import java.util.List;
import java.util.Objects;

public final class Wave {

    private final int number;
    private final List<WaveZombieEntry> zombies;
    private final long startDelayTicks;
    private final long spawnIntervalTicks;
    private final boolean finalWave;


    public Wave(
            int number,
            List<WaveZombieEntry> zombies
    ) {
        this(number, zombies, 0, 0, false);
    }

    public Wave(
            int number,
            List<WaveZombieEntry> zombies,
            long startDelayTicks,
            long spawnIntervalTicks,
            boolean finalWave
    ) {
        if (number <= 0) {
            throw new IllegalArgumentException(
                    "wave number must be positive"
            );
        }

        this.number = number;

        this.zombies = List.copyOf(
                Objects.requireNonNull(
                        zombies,
                        "zombies cannot be null"
                )
        );
        if (startDelayTicks < 0 || spawnIntervalTicks < 0) {
            throw new IllegalArgumentException(
                    "wave delays cannot be negative"
            );
        }
        this.startDelayTicks = startDelayTicks;
        this.spawnIntervalTicks = spawnIntervalTicks;
        this.finalWave = finalWave;
    }


    public int getNumber() {
        return number;
    }


    public List<WaveZombieEntry> getZombies() {
        return zombies;
    }


    public boolean isFinalWave() {
        return finalWave;
    }

    public long getStartDelayTicks() {
        return startDelayTicks;
    }

    public long getSpawnIntervalTicks() {
        return spawnIntervalTicks;
    }
}
