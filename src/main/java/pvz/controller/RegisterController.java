package pvz.controller;

import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.service.AuthService;
import pvz.model.command.RegisterCommand;
import pvz.model.command.Command;
import pvz.model.utils.*;
import pvz.view.MenuView;

public class RegisterController extends BaseController {
    private final AuthService authService;
    private User pendingUser = null;

    public RegisterController(AppState appState, UserManager userManager, MenuView view) {
        super(appState, userManager, view);
        this.authService = new AuthService(userManager);
    }

    @Override
    protected Message handleSpecificCommand(Command command) {
        if (command instanceof RegisterCommand regCommand) {
            switch (regCommand.getAction()) {
                case REGISTER -> processRegistration(regCommand);
                case PICK_QUESTION -> processPickQuestion(regCommand);
            }
        } else {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
        }
        return null;
    }

    private void processRegistration(RegisterCommand reg) {

        this.pendingUser = null;

        try {
            this.pendingUser = authService.createDraftUser(
                    reg.getUsername(), reg.getPassword(), reg.getPasswordConfirm(),
                    reg.getNickname(), reg.getEmail(), reg.getGender());
            view.showSuccess(SystemMessage.SUCCESS_REGISTRATION.getMessage());
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    private void processPickQuestion(RegisterCommand reg) {
        if (pendingUser == null) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return;
        }
        try {
            authService.setSecurityInfo(
                    pendingUser, reg.getQuestionNumber(), reg.getAnswer(), reg.getAnswerConfirm());


            userManager.add(pendingUser);
            userManager.save();

            this.pendingUser = null;
            appState.setCurrentMenu(MenuName.LOGIN);
            view.showSuccess(SystemMessage.SUCCESS_CREATION.getMessage());
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }
}