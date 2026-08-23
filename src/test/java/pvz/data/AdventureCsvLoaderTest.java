package pvz.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import pvz.model.adventure.AdventureData;

class AdventureCsvLoaderTest {
    @Test
    void loadsNormalLevelsAndTheirWaveConfigurations() throws IOException {
        ZombieData zombieData = ZombieCsvLoader.load(
                "assets/Data/zombies.csv"
        );

        AdventureData data = AdventureCsvLoader.load(
                "assets/Data/chapters.csv",
                "assets/Data/levels.csv",
                "assets/Data/level_zombies.csv",
                "assets/Data/waves.csv",
                zombieData
        );

        assertEquals(4, data.catalog().chapters().size());
        assertEquals(
                2,
                data.catalog().levelsInChapter("ancient-egypt").size()
        );
        assertNotNull(data.wavesByLevelId().get("egypt-1"));
        assertEquals(
                3,
                data.wavesByLevelId().get("egypt-1").waves().size()
        );
    }
}
