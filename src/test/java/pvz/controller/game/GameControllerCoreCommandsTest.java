package pvz.controller.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.HypnosisService;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieFactory;
import pvz.model.entity.projectile.ProjectileType;
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
        session = createSession(List.of("Peashooter"));
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
    void mapShowsBothAllegiancesWhenOpposingZombiesShareATile() {
        Zombie ally = session.createZombie("default");
        ally.spawn(session.world(), 4, 1);
        HypnosisService.hypnotize(
                ally,
                session.game().getCurrentTick()
        );
        Zombie hostile = session.createZombie("default");
        hostile.spawn(session.world(), 4, 1);

        String map = controller.handle("show map");

        assertTrue(map.contains("[X]"));
        assertTrue(map.contains("X = opposing zombies sharing a tile"));
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

    @Test
    void brokenArmorIsNotReportedAsAnActiveArmorLayer() {
        Zombie cone = session.createZombie("cone head");
        cone.spawn(session.world(), 9, 1);
        ProjectileType.NORMAL.hitZombie(
                cone,
                cone.getArmorHealth(),
                session.game().getCurrentTick()
        );

        String zombieInfo = controller.handle("zombies info");

        assertTrue(zombieInfo.contains("\t\tnone"));
        assertFalse(zombieInfo.contains("\t\tcone:"));
    }

    @Test
    void feedingFrozenPlantDoesNotConsumePlantFood() {
        session.world().sunBank().add(100);
        String planted = controller.handle(
                "plant plant -t Peashooter -l (1, 1)"
        );
        assertTrue(planted.startsWith("planted Peashooter"));
        Plant plant = session.board().getTopPlant(1, 1);
        assertTrue(session.board().addPlantFreezeLevel(
                plant,
                Plant.FULL_FREEZE_LEVEL
        ));
        assertTrue(session.resources().tryAddPlantFood());

        String result = controller.handle("feed plant -l (1, 1)");

        assertTrue(result.contains("plant food cannot be applied"));
        assertEquals(1, session.resources().getPlantFoodCount());
        assertFalse(plant.isPlantFoodActive(session.game().getCurrentTick()));
    }

    @Test
    void goldBloomStaysFiveTicksAfterPlantingAndIsThenRemoved()
            throws IOException {
        GameSession goldBloomSession = createSession(
                List.of("Gold Bloom")
        );
        goldBloomSession.start();
        GameController goldBloomController = new GameController(
                goldBloomSession
        );
        int registeredBeforePlanting = goldBloomSession.game()
                .getRegisteredObjectCount();

        String result = goldBloomController.handle(
                "plant plant -t Gold Bloom -l (3, 2)"
        );

        assertTrue(result.startsWith("planted Gold Bloom"));
        assertEquals(
                1,
                goldBloomSession.board().getTile(3, 2).getPlants().size()
        );
        assertEquals(5, goldBloomSession.world().getCollectibles().size());
        assertEquals(
                registeredBeforePlanting + 6,
                goldBloomSession.game().getRegisteredObjectCount()
        );

        goldBloomSession.advance(4);

        assertEquals(
                1,
                goldBloomSession.board().getTile(3, 2).getPlants().size()
        );

        goldBloomSession.advance(1);

        assertTrue(
                goldBloomSession.board().getTile(3, 2)
                        .getPlants()
                        .isEmpty()
        );
        assertEquals(5, goldBloomSession.world().getCollectibles().size());
        assertEquals(
                registeredBeforePlanting + 5,
                goldBloomSession.game().getRegisteredObjectCount()
        );
    }

    private GameSession createSession(
            List<String> selectedPlants
    ) throws IOException {
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
                selectedPlants,
                Set.of(),
                0,
                3
        );

        return new GameSessionFactory(
                new PlantFactory(
                        PlantCsvLoader.load(
                                "assets/Data/plants.csv"
                        ).byName()
                ),
                new ZombieFactory(zombieData)
        ).create(config);
    }
}
