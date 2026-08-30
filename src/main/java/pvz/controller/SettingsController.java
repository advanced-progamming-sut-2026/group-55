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

    public SettingsController(AppState appState,UserManager userManager,MenuView view){
        super(appState,userManager,view);
    }

    @Override
    protected Message handleSpecificCommand(Command command){
        if(!(command instanceof SettingsCommand settingsCmd)){
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }

        User currentUser=appState.getCurrentUser();

        if(currentUser==null){
            view.showError(SystemMessage.USER_NOT_LOGGED_IN.getMessage());
            return null;
        }

        switch(settingsCmd.getAction()){
            case CHANGE_DIFFICULTY -> {
                int level=settingsCmd.getLevel();

                if(level<1||level>5){
                    view.showError(SystemMessage.INVALID_DIFFICULTY.getMessage());
                }else{
                    currentUser.setDifficultyLevel(level);
                    userManager.save();
                    view.showSuccess("Difficulty level set to "+level);
                }
            }

            case CHANGE_GAME_SPEED -> {
                int speed=settingsCmd.getLevel();

                if(speed<1||speed>3){
                    view.showError("Game speed must be between 1 and 3.");
                }else{
                    currentUser.setGameSpeed(speed);
                    userManager.save();
                    view.showSuccess("Game speed set to "+speed);
                }
            }

            case TOGGLE_GRID -> {
                currentUser.setShowGrid(settingsCmd.isEnabled());
                userManager.save();
                view.showSuccess("Grid display "+(settingsCmd.isEnabled()?"enabled.":"disabled."));
            }

            case TOGGLE_DEBUG -> {
                currentUser.setDebugMode(settingsCmd.isEnabled());
                userManager.save();
                view.showSuccess("Debug mode "+(settingsCmd.isEnabled()?"enabled.":"disabled."));
            }

            default -> view.showError(SystemMessage.INVALID_COMMAND.getMessage());
        }

        return null;
    }
}
