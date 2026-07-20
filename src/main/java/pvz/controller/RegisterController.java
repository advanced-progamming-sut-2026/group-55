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

        if (command instanceof RegisterCommand) {
            RegisterCommand regCommand = (RegisterCommand) command;

            switch (regCommand.getAction()) {
                case REGISTER:
                    processRegistration(regCommand);
                    break;
                case PICK_QUESTION:
                    processPickQuestion(regCommand);
                    break;
            }
        } else {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
        }

        return null;
    }

    private void processRegistration(RegisterCommand reg) {
        try {
            this.pendingUser = authService.processInitialRegistration(
                    reg.getUsername(), reg.getPassword(), reg.getPasswordConfirm(),
                    reg.getNickname(), reg.getEmail(), reg.getGender());
            view.showSuccess(SystemMessage.SUCCESS_REGISTRATION.getMessage());
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    private void processPickQuestion(RegisterCommand reg) {
        try {
            authService.saveQuestionAnswer(reg.getQuestionNumber(), reg.getAnswer(), reg.getAnswerConfirm());
            view.showSuccess(SystemMessage.SUCCESS_CREATION.getMessage());
        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }
}