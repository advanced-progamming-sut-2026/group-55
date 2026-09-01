package pvz.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.LoginCommand;
import pvz.model.service.AuthService;
import pvz.model.utils.AppState;
import pvz.view.MenuView;

class LoginControllerRecoveryTest {

    @Test
    void cancelRecoveryAllowsReturningToLogin(@TempDir Path tempDirectory) {
        UserManager userManager = new UserManager(
                tempDirectory.resolve("save.json").toString()
        );
        AuthService authService = new AuthService(userManager);
        User user = new User(
                "player",
                authService.hashPasswordSHA256("StrongPass1!"),
                "Player",
                "player@example.com",
                "Male"
        );
        user.setSecurityQuestionNumber(1);
        user.setSecurityAnswer("green");
        userManager.add(user);

        LoginController controller = new LoginController(
                new AppState(),
                userManager,
                authService,
                new SilentView()
        );

        controller.handle(LoginCommand.createForgetPassword(
                "player",
                "player@example.com"
        ));
        controller.handle(LoginCommand.createAnswer("green"));

        assertTrue(controller.isRecoveryUserFound());
        assertTrue(controller.isWaitingForNewPassword());

        controller.cancelRecovery();

        assertFalse(controller.isRecoveryUserFound());
        assertFalse(controller.isWaitingForNewPassword());
    }

    private static class SilentView implements MenuView {
        @Override
        public void showSuccess(String message) {
        }

        @Override
        public void showError(String message) {
        }

        @Override
        public void showMessage(String message) {
        }

        @Override
        public void showRegisterWelcome() {
        }
    }
}
