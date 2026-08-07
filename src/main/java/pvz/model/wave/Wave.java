package pvz.model.wave;

import java.util.List;
import java.util.Objects;

public final class Wave {

    private final int number;
    private final List<WaveZombieEntry> zombies;


    public Wave(
            int number,
            List<WaveZombieEntry> zombies
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
    }


    public int getNumber() {
        return number;
    }


    public List<WaveZombieEntry> getZombies() {
        return zombies;
    }


    public boolean isFinalWave() {
        return false;
    }
}
