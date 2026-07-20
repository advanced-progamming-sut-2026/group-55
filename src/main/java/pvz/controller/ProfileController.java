package pvz.controller;

import pvz.model.command.Command;
import pvz.model.command.ProfileCommand;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.utils.AppState;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

public class ProfileController extends BaseController {

    public ProfileController(AppState appState, UserManager userManager, MenuView view) {
        super(appState, userManager, view);
    }

    @Override
    protected Message handleSpecificCommand(Command command) {

        if (!(command instanceof ProfileCommand profileCmd)) return null;

        User currentUser = appState.getCurrentUser();

        switch (profileCmd.getAction()) {
            case CHANGE_USERNAME -> {
                if (currentUser.getUsername().equals(profileCmd.getNewUsername())) {
                    view.showError(SystemMessage.PROFILE_SAME_USERNAME.getMessage());
                } else {
                    currentUser.setUsername(profileCmd.getNewUsername());
                    userManager.save();
                    view.showSuccess(SystemMessage.PROFILE_USERNAME_CHANGED.getMessage());
                }
            }

            case CHANGE_NICKNAME -> {
                if (profileCmd.getNewNickname().equals(currentUser.getNickname())) {
                    view.showError(SystemMessage.PROFILE_SAME_NICKNAME.getMessage());
                } else {
                    currentUser.setNickname(profileCmd.getNewNickname());
                    userManager.save();
                    view.showSuccess(SystemMessage.PROFILE_NICKNAME_CHANGED.getMessage());
                }
            }

            case CHANGE_EMAIL -> {
                if (profileCmd.getNewEmail().equals(currentUser.getEmail())) {
                    view.showError(SystemMessage.PROFILE_SAME_EMAIL.getMessage());
                } else {
                    currentUser.setEmail(profileCmd.getNewEmail());
                    userManager.save();
                    view.showSuccess(SystemMessage.PROFILE_EMAIL_CHANGED.getMessage());
                }
            }

            case CHANGE_PASSWORD -> {

                if (!currentUser.getPassword().equals(profileCmd.getOldPassword())) {
                    view.showError(SystemMessage.PROFILE_WRONG_OLD_PASSWORD.getMessage());
                } else if (currentUser.getPassword().equals(profileCmd.getNewPassword())) {
                    view.showError(SystemMessage.PROFILE_SAME_PASSWORD.getMessage());
                } else {
                    currentUser.setPassword(profileCmd.getNewPassword());
                    userManager.save();
                    view.showSuccess(SystemMessage.PROFILE_PASSWORD_CHANGED.getMessage());
                }
            }

            case SHOW_INFO -> {
                String info = "Username: " + currentUser.getUsername() + "\n" +
                        "Nickname: " + currentUser.getNickname() + "\n" +
                        "Games Played: " + currentUser.getGamesPlayed() + "\n" +
                        "Coins: " + currentUser.getCoins() + "\n" +
                        "Gems: " + currentUser.getDiamonds() + "\n" +
                        "Passed Levels: " + currentUser.getClearedStages() + "\n" +
                        "Highest Score: " + currentUser.getMaxMewPoint();
                view.showMessage(info);
            }
        }
        return null;
    }
}