package pvz.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;
import pvz.view.MenuView;
import pvz.view.commandparser.MainMenuParser;

/** Console compatibility around the graphical Phase-2 minigames shell. */
class MinigamesNavigationCompatibilityTest {
    @TempDir
    Path tempDirectory;

    @Test
    void mainMenuCanEnterMinigamesAndMinigamesExitReturnsMain() {
        AppState appState = new AppState();
        appState.setCurrentMenu(MenuName.MAIN);

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

        Command command = new MainMenuParser().parse(
                "menu enter minigames"
        );
        assertInstanceOf(Command.MenuEnterCommand.class, command);

        CapturingView view = new CapturingView();
        MainMenuController main = new MainMenuController(
                appState,
                userManager,
                view
        );
        main.handle(command);
        assertEquals(MenuName.MINIGAMES, appState.getCurrentMenu());

        MinigamesController minigames = new MinigamesController(
                appState,
                userManager,
                view
        );
        minigames.handle(new Command.MenuExitCommand());
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
