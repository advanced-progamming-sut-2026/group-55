package pvz.model.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pvz.data.AdventureCsvLoader;
import pvz.data.PlantCsvLoader;
import pvz.data.ZombieCsvLoader;
import pvz.data.ZombieData;
import pvz.model.adventure.AdventureData;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.ZombieFactory;

class GameRuntimeRestartTest {
    private GameRuntime runtime;
    private GameSessionConfig config;

    @BeforeEach
    void setUp() throws IOException {
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
        config = new GameSessionConfigFactory(adventureData).create(
                "egypt-1",
                List.of("Peashooter"),
                Set.of(),
                2,
                3
        );
        runtime = new GameRuntime(new GameSessionFactory(
                new PlantFactory(
                        PlantCsvLoader.load(
                                "assets/Data/plants.csv"
                        ).byName()
                ),
                new ZombieFactory(zombieData)
        ));
    }

    @Test
    void restartAbortsOldBattleAndCreatesFreshState() {
        runtime.start(config);
        GameSession previous = runtime.session();
        previous.resources().sunBank().add(250);
        runtime.handle("plant plant -t Peashooter -l (1, 1)");
        assertNotNull(previous.board().getTopPlant(1, 1));
        previous.advance(3);

        runtime.restart(config);
        GameSession restarted = runtime.session();

        assertEquals(GameSessionStatus.ABORTED, previous.status());
        assertTrue(restarted.isRunning());
        assertNotSame(previous, restarted);
        assertNotSame(previous.world(), restarted.world());
        assertNotSame(previous.board(), restarted.board());
        assertNull(restarted.board().getTopPlant(1, 1));
        assertEquals(0L, restarted.game().getCurrentTick());
        assertEquals(config.startingSun(),
                restarted.resources().sunBank().getBalance());
        assertEquals(config.startingPlantFood(),
                restarted.resources().getPlantFoodCount());
    }

    @Test
    void restartAlsoReplacesAnAlreadyFinishedBattle() {
        runtime.start(config);
        GameSession previous = runtime.session();
        previous.abort();

        runtime.restart(config);

        assertEquals(GameSessionStatus.ABORTED, previous.status());
        assertTrue(runtime.session().isRunning());
        assertNotSame(previous, runtime.session());
    }
}
