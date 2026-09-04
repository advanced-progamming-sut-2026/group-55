package pvz.controller;

import pvz.model.command.LoginCommand;
import pvz.model.command.Command;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.service.AuthService;
import pvz.model.utils.*;
import pvz.view.MenuView;

public class LoginController extends BaseController {

    private final AuthService authService;

    private User recoveryUser = null;
    private boolean isWaitingForNewPassword = false;

    public LoginController(
            AppState appState,
            UserManager userManager,
            AuthService authService,
            MenuView view
    ) {
        super(appState, userManager, view);
        this.authService = authService;
    }

    @Override
    protected Message handleSpecificCommand(Command command) {

        if (isWaitingForNewPassword) {

            if (command instanceof Command.RawTextCommand rawTextCommand) {
                handleNewPassword(rawTextCommand.getText());
            } else {
                view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            }

            return null;
        }

        if (!(command instanceof LoginCommand loginCmd)) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }

        switch (loginCmd.getAction()) {

            case LOGIN:
                processLogin(loginCmd);
                break;

            case FORGET_PASSWORD:
                processForgetPassword(loginCmd);
                break;

            case ANSWER:
                processAnswer(loginCmd);
                break;
        }

        return null;
    }

    private void handleNewPassword(String newPassword) {

        SystemMessage passErr =
                AuthValidator.getPasswordWeaknessReason(newPassword);

        if (passErr != null) {
            view.showError(passErr.getMessage());
            return;
        }

        if (recoveryUser == null) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return;
        }

        recoveryUser.setPassword(
                authService.hashPasswordSHA256(newPassword)
        );

        userManager.save();

        resetRecoveryState();

        view.showSuccess(
                SystemMessage.PASSWORD_CHANGED_SUCCESS.getMessage()
        );
    }

    private void processLogin(LoginCommand login) {

        User user = userManager.find(
                u -> u.getUsername().equals(login.getUsername())
        );

        if (user == null ||
                !user.getPassword().equals(
                        authService.hashPasswordSHA256(login.getPassword())
                )) {

            view.showError(
                    SystemMessage.LOGIN_FAILED.getMessage()
            );

            return;
        }

        appState.setCurrentUser(user);

        boolean keepLoggedIn = login.isStayLoggedIn();

        for (User u : userManager.getAll()) {
            u.setStayLoggedIn(false);
        }

        user.setStayLoggedIn(keepLoggedIn);

        userManager.save();

        appState.setCurrentMenu(MenuName.MAIN);

        view.showSuccess(
                SystemMessage.LOGIN_SUCCESS.getMessage()
        );

        view.showMessage("--- MAIN MENU ---");

        for (String command : MenuHelp.MAIN) {
            view.showMessage(command);
        }
    }

    private void processForgetPassword(LoginCommand forget) {

        User user = userManager.find(
                u -> u.getUsername().equals(forget.getUsername())
        );

        if (user == null ||
                !user.getEmail().equals(forget.getEmail())) {

            view.showError(
                    SystemMessage.FORGET_PASS_FAILED.getMessage()
            );

            return;
        }

        recoveryUser = user;

        String questionText =
                SystemMessage.getSecurityQuestion(
                        user.getSecurityQuestionNumber()
                );

        view.showMessage(questionText);
    }

    private void processAnswer(LoginCommand answerCmd) {

        if (recoveryUser == null) {
            view.showError(
                    SystemMessage.INVALID_COMMAND.getMessage()
            );

            return;
        }

        if (!recoveryUser.getSecurityAnswer()
                .equals(answerCmd.getAnswer())) {

            view.showError(
                    SystemMessage.ANSWER_INCORRECT.getMessage()
            );

            return;
        }

        isWaitingForNewPassword = true;

        view.showMessage(
                SystemMessage.ENTER_NEW_PASSWORD.getMessage()
        );
    }

    public boolean isRecoveryUserFound() {
        return recoveryUser != null;
    }

    public boolean isWaitingForNewPassword() {
        return isWaitingForNewPassword;
    }

    public void cancelRecovery() {
        resetRecoveryState();
    }

    private void resetRecoveryState() {
        recoveryUser = null;
        isWaitingForNewPassword = false;
    }
}
