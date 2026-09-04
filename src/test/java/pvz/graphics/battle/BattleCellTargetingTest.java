package pvz.graphics.battle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pvz.controller.game.GameController;
import pvz.data.AdventureCsvLoader;
import pvz.data.PlantCsvLoader;
import pvz.data.ZombieCsvLoader;
import pvz.data.ZombieData;
import pvz.model.adventure.AdventureData;
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.ZombieFactory;
import pvz.model.session.GameSession;
import pvz.model.session.GameSessionConfig;
import pvz.model.session.GameSessionConfigFactory;
import pvz.model.session.GameSessionFactory;

class BattleCellTargetingTest {
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
        session.resources().sunBank().add(1000);
        controller = new GameController(session);
    }

    @Test
    void plantPreviewUsesTheSamePlacementAndResourceRules() {
        Plant candidate = session.createPlant("Peashooter");

        assertTrue(BattleCellTargeting.canPlant(
                session, "Peashooter", candidate, 1, 1
        ));

        assertTrue(controller.handle(
                "plant plant -t Peashooter -l (1, 1)"
        ).startsWith("planted Peashooter"));
        assertFalse(BattleCellTargeting.canPlant(
                session, "Peashooter", candidate, 1, 1
        ));
        assertFalse(BattleCellTargeting.canPlant(
                session, "Peashooter", candidate, 2, 1
        ));

        session.resources().enableCooldownCheat();
        session.board().setTileType(2, 1, TileType.TOMBSTONE);
        assertFalse(BattleCellTargeting.canPlant(
                session, "Peashooter", candidate, 2, 1
        ));

        session.board().setGroundOccupancy((column, row) ->
                column == 3 && row == 1);
        assertFalse(BattleCellTargeting.canPlant(
                session, "Peashooter", candidate, 3, 1
        ));
        assertFalse(BattleCellTargeting.canPlant(
                session, "Peashooter", candidate, 10, 1
        ));
    }

    @Test
    void shovelPreviewRequiresARemovableTopPlant() {
        assertFalse(BattleCellTargeting.canShovel(
                session.board(), 1, 1
        ));

        assertTrue(controller.handle(
                "plant plant -t Peashooter -l (1, 1)"
        ).startsWith("planted Peashooter"));
        assertTrue(BattleCellTargeting.canShovel(
                session.board(), 1, 1
        ));

        Plant plant = session.board().getTopPlant(1, 1);
        assertTrue(plant.tryApplyPlantFood(session.game().getCurrentTick()));
        assertFalse(BattleCellTargeting.canShovel(
                session.board(), 1, 1
        ));
    }

    @Test
    void plantFoodPreviewChecksInventoryAndPlantCompatibility() {
        assertTrue(controller.handle(
                "plant plant -t Peashooter -l (1, 1)"
        ).startsWith("planted Peashooter"));
        assertFalse(BattleCellTargeting.canUsePlantFood(session, 1, 1));

        assertTrue(session.resources().tryAddPlantFood());
        assertTrue(BattleCellTargeting.canUsePlantFood(session, 1, 1));
        assertFalse(BattleCellTargeting.canUsePlantFood(session, 2, 1));

        Plant plant = session.board().getTopPlant(1, 1);
        assertTrue(session.board().addPlantFreezeLevel(
                plant,
                Plant.FULL_FREEZE_LEVEL
        ));
        assertFalse(BattleCellTargeting.canUsePlantFood(session, 1, 1));
    }
}
