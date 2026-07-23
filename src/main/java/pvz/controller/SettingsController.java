package pvz.controller;

import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.command.Command;
import pvz.model.command.SettingsCommand;
import pvz.model.utils.AppState;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

public class SettingsController extends BaseController {

    public SettingsController(AppState appState, UserManager userManager, MenuView view) {
        super(appState, userManager, view);
    }

    @Override
    protected Message handleSpecificCommand(Command command) {
        if (!(command instanceof SettingsCommand settingsCmd)) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }

        switch (settingsCmd.getAction()) {
            case CHANGE_DIFFICULTY -> {
                int level = settingsCmd.getLevel();
                User currentUser = appState.getCurrentUser();

                if (level < 1 || level > 5) {
                    view.showError(SystemMessage.INVALID_DIFFICULTY.getMessage());
                } else if (currentUser == null) {
                    view.showError(SystemMessage.USER_NOT_LOGGED_IN.getMessage());
                } else {
                    currentUser.setDifficultyLevel(level);
                    userManager.updateDifficulty(currentUser.getUsername(), level);
                    view.showSuccess("Difficulty level set to " + level);
                }
            }
            default -> view.showError(SystemMessage.INVALID_COMMAND.getMessage());
        }
        return null;
    }
}