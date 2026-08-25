package pvz.model.wave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pvz.data.AdventureCsvLoader;
import pvz.data.ZombieCsvLoader;
import pvz.data.ZombieData;
import pvz.model.adventure.AdventureData;
import pvz.model.entity.zombie.ZombieFactory;

class WaveGeneratorTest {
    private static AdventureData adventureData;
    private static ZombieFactory zombieFactory;

    @BeforeAll
    static void loadData() throws IOException {
        ZombieData zombieData = ZombieCsvLoader.load(
                "assets/Data/zombies.csv"
        );
        zombieFactory = new ZombieFactory(zombieData);
        adventureData = AdventureCsvLoader.load(
                "assets/Data/chapters.csv",
                "assets/Data/levels.csv",
                "assets/Data/level_zombies.csv",
                "assets/Data/waves.csv",
                zombieData
        );
    }

    @Test
    void fillsEveryBudgetExactlyAtEveryDifficulty() {
        WaveConfiguration configuration =
                adventureData.wavesByLevelId().get("egypt-1");

        for (int difficulty = 1; difficulty <= 5; difficulty++) {
            List<Wave> waves = new WaveGenerator(
                    zombieFactory,
                    new Random(100 + difficulty)
            ).generate(configuration, 5, difficulty);

            for (int index = 0; index < waves.size(); index++) {
                int expectedBudget = configuration.waves()
                        .get(index)
                        .budget();
                int actualBudget = waves.get(index).getZombies().stream()
                        .mapToInt(WaveZombieEntry::cost)
                        .sum();
                assertEquals(expectedBudget, actualBudget);
                assertTrue(waves.get(index).getZombies().stream()
                        .allMatch(entry -> entry.lane() >= 1
                                && entry.lane() <= 5));
            }
        }
    }
}
