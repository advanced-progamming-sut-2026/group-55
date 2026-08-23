package pvz.model.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import pvz.data.AdventureCsvLoader;
import pvz.data.PlantCsvLoader;
import pvz.data.ZombieCsvLoader;
import pvz.data.ZombieData;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;
import pvz.model.adventure.AdventureData;

class LawnMowerLossFlowTest {
    @Test
    void firstBreachConsumesMowerAndSecondBreachLosesLevel()
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
                        PlantCsvLoader.load(
                                "assets/Data/plants.csv"
                        ).byName()
                ),
                new ZombieFactory(zombieData)
        ).create(config);
        session.start();

        Zombie first = session.createZombie("default");
        first.spawn(session.world(), 1, 1);
        session.advance(30);

        assertTrue(session.isRunning());
        assertFalse(session.world().isLawnMowerAvailable(1));

        Zombie second = session.createZombie("default");
        second.spawn(session.world(), 1, 1);
        session.advance(30);

        assertEquals(GameSessionStatus.LOST, session.status());
    }
}
