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

    public LoginController(AppState appState, UserManager userManager, AuthService authService, MenuView view) {
        super(appState, userManager, view);
        this.authService = authService;
    }

    @Override
    protected Message handleSpecificCommand(Command command) {

        if (isWaitingForNewPassword && command instanceof Command.RawTextCommand) {
            handleNewPassword(((Command.RawTextCommand) command).getText());
            return null;
        }

        if (command instanceof LoginCommand) {
            LoginCommand loginCmd = (LoginCommand) command;

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
        } else {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
        }

        return null;
    }

    private void handleNewPassword(String newPassword) {
        SystemMessage passErr = AuthValidator.getPasswordWeaknessReason(newPassword);
        if (passErr != null) {
            view.showError(passErr.getMessage());
        } else {
            recoveryUser.setPassword(authService.hashPasswordSHA256(newPassword));
            resetRecoveryState();
            view.showSuccess(SystemMessage.PASSWORD_CHANGED_SUCCESS.getMessage());
        }
    }

    private void processLogin(LoginCommand login) {
        resetRecoveryState();
        User user = userManager.find(u -> u.getUsername().equals(login.getUsername()));

        if (user == null || !user.getPassword().equals(authService.hashPasswordSHA256(login.getPassword()))) {
            view.showError(SystemMessage.LOGIN_FAILED.getMessage());
        } else {
            appState.setCurrentUser(user);
            if (login.isStayLoggedIn()) {
                user.setStayLoggedIn(true);
                userManager.save();
            }
            appState.setCurrentMenu(MenuName.MAIN);
            view.showSuccess(SystemMessage.LOGIN_SUCCESS.getMessage());
        }
    }

    private void processForgetPassword(LoginCommand forget) {
        resetRecoveryState();
        User user = userManager.find(u -> u.getUsername().equals(forget.getUsername()));

        if (user == null || !user.getEmail().equals(forget.getEmail())) {
            view.showError(SystemMessage.FORGET_PASS_FAILED.getMessage());
        } else {
            this.recoveryUser = user;
            view.showMessage(user.getSecurityQuestionNumber() + " - Please answer your security answer.");
        }
    }

    private void processAnswer(LoginCommand answerCmd) {
        if (recoveryUser == null) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
        } else {
            if (recoveryUser.getSecurityAnswer().equals(answerCmd.getAnswer())) {
                isWaitingForNewPassword = true;
                view.showMessage(SystemMessage.ENTER_NEW_PASSWORD.getMessage());
            } else {
                resetRecoveryState();
                view.showError(SystemMessage.ANSWER_INCORRECT.getMessage());
            }
        }
    }

    private void resetRecoveryState() {
        this.recoveryUser = null;
        this.isWaitingForNewPassword = false;
    }
}