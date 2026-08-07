package pvz.model.wave;

import java.util.List;
import java.util.Objects;

import pvz.model.core.GameEvents;

public final class WaveManager {

    private final List<Wave> waves;

    private int currentWaveIndex;


    public WaveManager(List<Wave> waves) {
        this.waves = List.copyOf(
                Objects.requireNonNull(
                        waves,
                        "waves cannot be null"
                )
        );

        this.currentWaveIndex = 0;
    }


    public Wave startNextWave() {

        if (currentWaveIndex >= waves.size()) {
            return null;
        }

        Wave wave = waves.get(currentWaveIndex);

        currentWaveIndex++;


        GameEvents.publish(
                "Wave "
                        + wave.getNumber()
                        + " started."
        );


        if (!hasMoreWaves()) {
            GameEvents.publish(
                    "The final wave has come."
            );
        }


        for (WaveZombieEntry zombie : wave.getZombies()) {

            GameEvents.publish(
                    "Zombie of type "
                            + zombie.zombieType()
                            + " spawned at wave "
                            + wave.getNumber()
                            + " in lane "
                            + zombie.lane()
                            + " which costed "
                            + zombie.cost()
                            + "."
            );
        }


        return wave;
    }


    public Wave getCurrentWave() {

        if (currentWaveIndex >= waves.size()) {
            return null;
        }

        return waves.get(currentWaveIndex);
    }


    public boolean hasMoreWaves() {
        return currentWaveIndex < waves.size();
    }


    public int getCurrentWaveNumber() {
        return currentWaveIndex + 1;
    }


    public int getTotalWaves() {
        return waves.size();
    }
}
