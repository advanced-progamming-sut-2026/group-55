package pvz.controller.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;
import pvz.model.adventure.AdventureData;
import pvz.model.session.GameSession;
import pvz.model.session.GameSessionConfig;
import pvz.model.session.GameSessionConfigFactory;
import pvz.model.session.GameSessionFactory;

class GameControllerCoreCommandsTest {
    private GameSession session;
    private GameController controller;

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
        GameSessionConfig config = new GameSessionConfigFactory(
                adventureData
        ).create(
                "egypt-1",
                List.of("Peashooter"),
                Set.of(),
                0,
                3
        );
        session = new GameSessionFactory(
                new PlantFactory(
                        PlantCsvLoader.load(
                                "assets/Data/plants.csv"
                        ).byName()
                ),
                new ZombieFactory(zombieData)
        ).create(config);
        session.start();
        controller = new GameController(session);
    }

    @Test
    void reportsPlantAvailabilityAndTileDetails() {
        String plantStatus = controller.handle("show plants status");

        assertTrue(plantStatus.contains("Peashooter"));
        assertTrue(plantStatus.contains("cost=100"));
        assertTrue(plantStatus.contains("status=ready"));

        controller.handle("plant plant -t Peashooter -l (1, 1)");
        String tileStatus = controller.handle(
                "show tile status -l (1, 1)"
        );

        assertTrue(tileStatus.contains("type: normal"));
        assertTrue(tileStatus.contains("Peashooter: health="));
        assertTrue(tileStatus.contains("category=shooter"));
    }

    @Test
    void showsMowerStateAndRemainingZombieEffectTime() {
        Zombie zombie = session.createZombie("default");
        zombie.spawn(session.world(), 4, 1);
        zombie.applyChill(0, 32);

        String map = controller.handle("show map");
        String zombieInfo = controller.handle("zombies info");

        assertTrue(map.contains("row 1=ready"));
        assertTrue(zombieInfo.contains("chilled: 3.2s remaining"));
    }

    @Test
    void nukeKillsEveryZombieOnTheBoard() {
        Zombie first = session.createZombie("default");
        Zombie second = session.createZombie("cone head");
        first.spawn(session.world(), 9, 1);
        second.spawn(session.world(), 9, 2);

        String result = controller.handle("release the nuke");

        assertTrue(result.contains("nuke released; killed 2 zombie(s)."));
        assertEquals(0, session.world().getZombies().size());
    }
}
