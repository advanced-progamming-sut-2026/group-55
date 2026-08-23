package pvz.model.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import pvz.data.AdventureCsvLoader;
import pvz.data.PlantCsvLoader;
import pvz.data.ZombieCsvLoader;
import pvz.data.ZombieData;
import pvz.model.adventure.AdventureData;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.ZombieFactory;

class NormalLevelSessionTest {
    @Test
    void winsOnlyAfterFinalWaveAndAllZombiesAreCleared()
            throws IOException {
        ZombieData zombieData = ZombieCsvLoader.load(
                "assets/Data/zombies.csv"
        );
        AdventureData adventureData = AdventureCsvLoader.load(
                "assets/Data/chapters.csv",
                "assets/Data/levels.csv",
                "assets/Data/level_zombies.csv",
                "assets/Data/waves.csv",
                zombieData
        );
        GameSessionConfig config = new GameSessionConfigFactory(
                adventureData
        ).create(
                "egypt-1",
                List.of("Peashooter"),
                Set.of(),
                0,
                3
        );
        GameSession session = new GameSessionFactory(
                new PlantFactory(
                        PlantCsvLoader.load("assets/Data/plants.csv").byName()
                ),
                new ZombieFactory(zombieData)
        ).create(config);
        session.start();

        for (int tick = 0; tick < 1000 && session.isRunning(); tick++) {
            session.advance(1);
            session.world().getZombies().forEach(
                    zombie -> zombie.takeDirectDamage(100_000)
            );
        }

        if (session.isRunning()) {
            session.advance(1);
        }
        assertTrue(session.isFinished());
        assertEquals(GameSessionStatus.WON, session.status());
    }
}
