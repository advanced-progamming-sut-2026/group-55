package pvz.controller;

import pvz.model.Command.Command;
import pvz.model.Command.SettingsCommand;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.utils.*;
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

                if (level >= 1 && level <= 5) {
                    appState.setDifficultyLevel(level);

                    User currentUser = appState.getCurrentUser();
                    if (currentUser != null) {
                        userManager.updateDifficulty(currentUser.getUsername(), level);
                    }
                    view.showSuccess("Difficulty level set to " + level);
                } else {
                    view.showError(SystemMessage.INVALID_DIFFICULTY.getMessage());
                }
            }

            default -> {
                view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            }
        }

        return null;
    }
}