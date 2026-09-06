package pvz.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pvz.data.AdventureCsvLoader;
import pvz.data.PlantCsvLoader;
import pvz.data.PlantData;
import pvz.data.ZombieCsvLoader;
import pvz.data.ZombieData;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.adventure.AdventureData;
import pvz.model.command.PlantSelectionCommand;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.ZombieFactory;
import pvz.model.session.GameRuntime;
import pvz.model.session.GameSessionConfigFactory;
import pvz.model.session.GameSessionFactory;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;
import pvz.view.MenuView;

class PlantSelectionBoostGuardTest {
    @Test
    void selectionStateIsExposedAsReadOnlyData() throws IOException {
        Fixture fixture = fixture("selection-user");

        fixture.controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.ADD_PLANT,
                "Peashooter"
        ));

        assertEquals(List.of("peashooter"), fixture.controller.getSelectedPlants());
        assertTrue(fixture.controller.isPlantSelected("Peashooter"));
        assertEquals(8, fixture.controller.getMaxSlots());
        boolean readOnly = false;
        try {
            fixture.controller.getSelectedPlants().add("sunflower");
        } catch (UnsupportedOperationException exception) {
            readOnly = true;
        }
        assertTrue(readOnly);
    }

    @Test
    void storedBoostOutsideLoadoutDoesNotBlockGameStart() throws IOException {
        Fixture fixture = fixture("stored-boost-user");
        fixture.user.addStoredBoost("Sunflower");

        fixture.controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.ADD_PLANT,
                "Peashooter"
        ));
        fixture.controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.START_GAME
        ));

        assertTrue(fixture.runtime.isActive());
        assertTrue(fixture.user.hasStoredBoost("Sunflower"));
        assertTrue(fixture.view.errors.isEmpty());
    }

    @Test
    void selectedStoredBoostActivatesStageWideOnFirstPlacement() throws IOException {
        Fixture fixture = fixture("stored-use-user");
        fixture.user.addStoredBoost("Sunflower");

        fixture.controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.ADD_PLANT,
                "Sunflower"
        ));
        fixture.controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.START_GAME
        ));

        assertTrue(fixture.runtime.isActive());
        assertTrue(fixture.user.hasStoredBoost("Sunflower"));
        assertTrue(fixture.runtime.session().hasPendingStoredBoost("Sunflower"));

        fixture.runtime.handle("plant plant -t Sunflower -l (99, 99)");
        assertTrue(fixture.user.hasStoredBoost("Sunflower"));
        assertTrue(fixture.runtime.session().hasPendingStoredBoost("Sunflower"));

        String result = fixture.runtime.handle(
                "plant plant -t Sunflower -l (1, 1)"
        );

        assertTrue(result.contains("Boost activated"));
        assertFalse(fixture.user.hasStoredBoost("Sunflower"));
        assertFalse(fixture.runtime.session().hasPendingStoredBoost("Sunflower"));
        assertTrue(fixture.runtime.session().isStoredBoostActivated("Sunflower"));
        assertTrue(fixture.runtime.session().isPlantBoosted("Sunflower"));

        fixture.runtime.handle("cheat remove-cooldown");
        String secondPlanting = fixture.runtime.handle(
                "plant plant -t Sunflower -l (2, 1)"
        );
        assertTrue(secondPlanting.contains("Boost activated"));
    }

    @Test
    void manualBoostDoesNotChargeUntilLoadoutIsCommitted() throws IOException {
        Fixture fixture = fixture("manual-boost-user");
        fixture.user.addDiamonds(10);

        fixture.controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.ADD_PLANT,
                "Peashooter"
        ));
        fixture.controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.BOOST_PLANT,
                "Peashooter"
        ));

        assertEquals(10, fixture.user.getDiamonds());
        assertEquals(2, fixture.controller.getPendingManualBoostCost());

        fixture.controller.resetSelection();

        assertEquals(10, fixture.user.getDiamonds());
        assertEquals(0, fixture.controller.getPendingManualBoostCost());
    }


    @Test
    void manualBoostIsChargedExactlyWhenLevelStarts() throws IOException {
        Fixture fixture = fixture("manual-start-user");
        fixture.user.addDiamonds(10);

        fixture.controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.ADD_PLANT,
                "Peashooter"
        ));
        fixture.controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.BOOST_PLANT,
                "Peashooter"
        ));
        fixture.controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.START_GAME
        ));

        assertTrue(fixture.runtime.isActive());
        assertEquals(8, fixture.user.getDiamonds());
        assertTrue(fixture.runtime.session().isPlantManuallyBoosted("Peashooter"));
    }

    private Fixture fixture(String username) throws IOException {
        PlantData plantData = PlantCsvLoader.load("assets/Data/plants.csv");
        ZombieData zombieData = ZombieCsvLoader.load("assets/Data/zombies.csv");
        AdventureData adventureData = AdventureCsvLoader.load(
                "assets/Data/chapters.csv",
                "assets/Data/levels.csv",
                "assets/Data/level_zombies.csv",
                "assets/Data/waves.csv",
                zombieData
        );
        GameRuntime runtime = new GameRuntime(new GameSessionFactory(
                new PlantFactory(plantData.byName()),
                new ZombieFactory(zombieData)
        ));
        AppState appState = new AppState();
        User user = new User(
                username,
                "hash",
                "Tester",
                username + "@example.com",
                "x"
        );
        appState.setCurrentUser(user);
        appState.setSelectedLevelId("egypt-1");
        appState.setCurrentMenu(MenuName.PLANT_SELECTION);
        RecordingView view = new RecordingView();
        Path savePath = Files.createTempDirectory("pvz-selection-")
                .resolve("users.json");
        UserManager userManager = new UserManager(savePath.toString());
        userManager.add(user);
        PlantSelectionController controller = new PlantSelectionController(
                appState,
                userManager,
                view,
                plantData,
                runtime,
                new GameSessionConfigFactory(adventureData)
        );
        return new Fixture(user, runtime, controller, view);
    }

    private record Fixture(
            User user,
            GameRuntime runtime,
            PlantSelectionController controller,
            RecordingView view
    ) {
    }

    private static final class RecordingView implements MenuView {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void showSuccess(String message) {
        }

        @Override
        public void showError(String errorMessage) {
            errors.add(errorMessage);
        }

        @Override
        public void showMessage(String message) {
        }

        @Override
        public void showRegisterWelcome() {
        }
    }
}
