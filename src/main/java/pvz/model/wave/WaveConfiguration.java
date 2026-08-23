package pvz.model.wave;

import java.util.List;
import java.util.Objects;

public record WaveConfiguration(
        List<String> allowedZombieIds,
        List<WaveConfig> waves
) {
    public WaveConfiguration {
        allowedZombieIds = List.copyOf(
                Objects.requireNonNull(
                        allowedZombieIds,
                        "allowed zombie ids cannot be null"
                )
        );
        waves = List.copyOf(
                Objects.requireNonNull(waves, "waves cannot be null")
        );
        if (allowedZombieIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "a level must allow at least one zombie"
            );
        }
        if (waves.isEmpty()) {
            throw new IllegalArgumentException(
                    "a level must contain at least one wave"
            );
        }
        validateWaveOrder(waves);
    }

    private static void validateWaveOrder(List<WaveConfig> waves) {
        for (int index = 0; index < waves.size(); index++) {
            WaveConfig wave = waves.get(index);
            int expectedNumber = index + 1;
            if (wave.number() != expectedNumber) {
                throw new IllegalArgumentException(
                        "wave numbers must be consecutive from 1"
                );
            }
            boolean shouldBeFinal = index == waves.size() - 1;
            if (wave.finalWave() != shouldBeFinal) {
                throw new IllegalArgumentException(
                        "only the last wave must be marked as final"
                );
            }
        }
    }
}
