package pvz.controller;

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
    void storedBoostedPlantMustBeSelectedBeforeStartingGame()
            throws IOException {
        PlantData plantData = PlantCsvLoader.load(
                "assets/Data/plants.csv"
        );
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
        GameRuntime runtime = new GameRuntime(new GameSessionFactory(
                new PlantFactory(plantData.byName()),
                new ZombieFactory(zombieData)
        ));
        AppState appState = new AppState();
        User user = new User(
                "test-user",
                "hash",
                "Tester",
                "tester@example.com",
                "x"
        );
        user.addStoredBoost("Sunflower");
        appState.setCurrentUser(user);
        appState.setSelectedLevelId("egypt-1");
        appState.setCurrentMenu(MenuName.PLANT_SELECTION);
        RecordingView view = new RecordingView();
        Path savePath = Files.createTempDirectory(
                "pvz-boost-guard-"
        ).resolve("users.json");
        PlantSelectionController controller = new PlantSelectionController(
                appState,
                new UserManager(savePath.toString()),
                view,
                plantData,
                runtime,
                new GameSessionConfigFactory(adventureData)
        );

        controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.ADD_PLANT,
                "Peashooter"
        ));
        controller.handle(new PlantSelectionCommand(
                PlantSelectionCommand.Action.START_GAME
        ));

        assertTrue(view.errors.stream().anyMatch(
                message -> message.contains("sunflower")
        ));
        assertTrue(appState.getCurrentMenu() == MenuName.PLANT_SELECTION);
        assertFalse(runtime.isActive());
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
