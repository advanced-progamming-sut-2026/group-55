package pvz.model.wave;

/** Read-only progress view for HUDs and other presentation layers. */
public record WaveProgressSnapshot(
        int currentWaveNumber,
        int totalWaves,
        WaveManager.WaveState state,
        int spawnedZombies,
        int totalZombiesInCurrentWave,
        double currentWaveProgress,
        double overallProgress,
        int nextWaveNumber,
        long ticksUntilNextWave,
        boolean nextWaveFinal
) {
    public WaveProgressSnapshot {
        if (totalWaves <= 0) {
            throw new IllegalArgumentException("total waves must be positive");
        }
        if (currentWaveNumber < 0 || currentWaveNumber > totalWaves) {
            throw new IllegalArgumentException("current wave is out of range");
        }
        if (spawnedZombies < 0 || totalZombiesInCurrentWave < 0
                || spawnedZombies > totalZombiesInCurrentWave) {
            throw new IllegalArgumentException("invalid zombie spawn progress");
        }
        if (currentWaveProgress < 0d || currentWaveProgress > 1d
                || overallProgress < 0d || overallProgress > 1d) {
            throw new IllegalArgumentException("wave progress must be in [0, 1]");
        }
        if (nextWaveNumber < 0 || nextWaveNumber > totalWaves) {
            throw new IllegalArgumentException("next wave is out of range");
        }
        if (ticksUntilNextWave < 0) {
            throw new IllegalArgumentException("next-wave delay cannot be negative");
        }
    }

    public boolean isWaitingForWave() {
        return state == WaveManager.WaveState.WAITING && nextWaveNumber > 0;
    }

    public boolean isCompleted() {
        return state == WaveManager.WaveState.COMPLETED;
    }
}
