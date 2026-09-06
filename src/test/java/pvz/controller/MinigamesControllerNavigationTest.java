package pvz.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;
import pvz.view.MenuView;

class MinigamesControllerNavigationTest {
    @TempDir
    Path tempDirectory;

    @Test
    void menuExitReturnsMinigamesShellToMainMenu() {
        AppState appState = new AppState();
        appState.setCurrentMenu(MenuName.MINIGAMES);

        UserManager userManager = new UserManager(
                tempDirectory.resolve("users.json").toString()
        );
        User user = new User(
                "player",
                "hash",
                "Player",
                "player@example.com",
                "x"
        );
        userManager.add(user);
        appState.setCurrentUser(user);

        MinigamesController controller = new MinigamesController(
                appState,
                userManager,
                new CapturingView()
        );
        controller.handle(new Command.MenuExitCommand());

        assertEquals(MenuName.MAIN, appState.getCurrentMenu());
    }

    private static final class CapturingView implements MenuView {
        @Override
        public void showSuccess(String message) {
        }

        @Override
        public void showError(String errorMessage) {
        }

        @Override
        public void showMessage(String message) {
        }

        @Override
        public void showRegisterWelcome() {
        }
    }
}
