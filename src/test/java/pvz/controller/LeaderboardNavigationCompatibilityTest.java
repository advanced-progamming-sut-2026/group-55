package pvz.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.adventure.LevelCatalog;
import pvz.model.command.Command;
import pvz.model.command.GameMenuCommand;
import pvz.model.utils.AppState;
import pvz.model.utils.MenuName;
import pvz.view.MenuView;
import pvz.view.commandparser.GameMenuParser;

class LeaderboardNavigationCompatibilityTest {
    @TempDir
    Path tempDirectory;

    @Test
    void legacyGameMenuCommandStillEntersAndExitsLeaderboard() {
        AppState appState = new AppState();
        appState.setCurrentMenu(MenuName.GAME);

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

        Command parsed = new GameMenuParser().parse("menu leaderboard");
        GameMenuCommand command = assertInstanceOf(
                GameMenuCommand.class,
                parsed
        );
        assertEquals(GameMenuCommand.Action.LEADERBOARD, command.getAction());

        CapturingView view = new CapturingView();
        GameMenuController gameController = new GameMenuController(
                appState,
                userManager,
                view,
                new LevelCatalog(Map.of(), Map.of())
        );
        gameController.handle(command);
        assertEquals(MenuName.LEADERBOARD, appState.getCurrentMenu());

        LeaderboardController leaderboardController =
                new LeaderboardController(appState, userManager, view);
        leaderboardController.handle(new Command.MenuExitCommand());
        assertEquals(MenuName.GAME, appState.getCurrentMenu());
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
