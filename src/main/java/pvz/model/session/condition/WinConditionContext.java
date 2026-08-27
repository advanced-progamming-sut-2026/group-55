package pvz.model.session.condition;

import java.util.Objects;
import pvz.model.core.World;
import pvz.model.wave.WaveManager;

public record WinConditionContext(
        World world,
        WaveManager waveManager,
        long currentTick
) {
    public WinConditionContext {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(
                waveManager,
                "wave manager cannot be null"
        );
        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }
    }

    public int aliveZombieCount() {
        return world.getHostileZombies().size();
    }
}
