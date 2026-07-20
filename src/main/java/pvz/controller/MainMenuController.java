package pvz.controller;

import pvz.model.command.Command;
import pvz.model.utils.*;
import pvz.model.account.UserManager;
import pvz.view.MenuView;

public class MainMenuController extends BaseController {

    public MainMenuController(AppState appState, UserManager userManager, MenuView view) {
        super(appState, userManager, view);
    }

    @Override
    protected Message handleSpecificCommand(Command command) {

        if (command instanceof Command.MenuLogoutCommand) {
            if (appState.getCurrentUser() != null) {
                appState.getCurrentUser().setStayLoggedIn(false);
                userManager.save();}

            appState.setCurrentUser(null);
            appState.setCurrentMenu(MenuName.REGISTER);
            view.showSuccess(SystemMessage.LOGOUT_SUCCESS.getMessage());
            return null;
        }

        if (command instanceof Command.MenuExitCommand) {
            view.showError(SystemMessage.LOGOUT_REQUIRED_MAIN.getMessage());
            return null;
        }

        view.showError(SystemMessage.INVALID_COMMAND.getMessage());
        return null;
    }
}