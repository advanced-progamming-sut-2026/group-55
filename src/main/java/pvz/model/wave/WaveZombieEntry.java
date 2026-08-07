package pvz.model.wave;

import java.util.Objects;

public record WaveZombieEntry(
        String zombieType,
        int lane,
        int cost
) {

    public WaveZombieEntry {
        Objects.requireNonNull(
                zombieType,
                "zombie type cannot be null"
        );

        if (lane <= 0) {
            throw new IllegalArgumentException(
                    "lane must be positive"
            );
        }

        if (cost < 0) {
            throw new IllegalArgumentException(
                    "cost cannot be negative"
            );
        }
    }
}
