package pvz.model.wave;

public record WaveConfig(
        int number,
        int budget,
        long startDelayTicks,
        long spawnIntervalTicks,
        boolean finalWave
) {
    public WaveConfig {
        if (number <= 0) {
            throw new IllegalArgumentException(
                    "wave number must be positive"
            );
        }
        if (budget <= 0) {
            throw new IllegalArgumentException(
                    "wave budget must be positive"
            );
        }
        if (startDelayTicks < 0 || spawnIntervalTicks < 0) {
            throw new IllegalArgumentException(
                    "wave delays cannot be negative"
            );
        }
    }
}
