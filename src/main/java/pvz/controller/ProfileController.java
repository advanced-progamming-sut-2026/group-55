package pvz.controller;

import pvz.model.command.Command;
import pvz.model.command.ProfileCommand;
import pvz.model.account.User;
import pvz.model.account.UserManager;
import pvz.model.service.AuthService;
import pvz.model.utils.AppState;
import pvz.model.utils.AuthValidator;
import pvz.model.utils.Message;
import pvz.model.utils.SystemMessage;
import pvz.view.MenuView;

public class ProfileController extends BaseController {

    private final AuthService authService;

    public ProfileController(AppState appState, UserManager userManager, AuthService authService, MenuView view) {
        super(appState, userManager, view);
        this.authService = authService;
    }

    @Override
    protected Message handleSpecificCommand(Command command) {
        if (!(command instanceof ProfileCommand profileCmd)) {
            view.showError(SystemMessage.INVALID_COMMAND.getMessage());
            return null;
        }

        User currentUser = appState.getCurrentUser();
        switch (profileCmd.getAction()) {
            case CHANGE_USERNAME -> handleChangeUsername(profileCmd, currentUser);
            case CHANGE_NICKNAME -> handleChangeNickname(profileCmd, currentUser);
            case CHANGE_EMAIL    -> handleChangeEmail(profileCmd, currentUser);
            case CHANGE_PASSWORD -> handleChangePassword(profileCmd, currentUser);
            case SHOW_INFO       -> handleShowInfo(currentUser);
        }
        return null;
    }

    private void handleChangeUsername(ProfileCommand cmd, User user) {
        String newUsername = cmd.getNewUsername();
        if (user.getUsername().equals(newUsername)) {
            view.showError(SystemMessage.PROFILE_SAME_USERNAME.getMessage());
        } else if (!AuthValidator.isValidUsername(newUsername)) {
            view.showError(SystemMessage.PROFILE_INVALID_USERNAME.getMessage());
        } else if (userManager.find(u -> u.getUsername().equals(newUsername)) != null) {
            view.showError(SystemMessage.PROFILE_USERNAME_EXISTS.getMessage());
        } else {
            user.setUsername(newUsername);
            userManager.save();
            view.showSuccess(SystemMessage.PROFILE_USERNAME_CHANGED.getMessage());
        }
    }

    private void handleChangeNickname(ProfileCommand cmd, User user) {
        String newNickname = cmd.getNewNickname();
        if (user.getNickname().equals(newNickname)) {
            view.showError(SystemMessage.PROFILE_SAME_NICKNAME.getMessage());
        } else if (!AuthValidator.isValidNickname(newNickname)) {
            view.showError(SystemMessage.PROFILE_INVALID_NICKNAME.getMessage());
        } else {
            user.setNickname(newNickname);
            userManager.save();
            view.showSuccess(SystemMessage.PROFILE_NICKNAME_CHANGED.getMessage());
        }
    }

    private void handleChangeEmail(ProfileCommand cmd, User user) {
        String newEmail = cmd.getNewEmail();
        if (user.getEmail().equals(newEmail)) {
            view.showError(SystemMessage.PROFILE_SAME_EMAIL.getMessage());
        } else if (!AuthValidator.isValidEmail(newEmail)) {
            view.showError(SystemMessage.PROFILE_INVALID_EMAIL.getMessage());
        } else {
            user.setEmail(newEmail);
            userManager.save();
            view.showSuccess(SystemMessage.PROFILE_EMAIL_CHANGED.getMessage());
        }
    }

    private void handleChangePassword(ProfileCommand cmd, User user) {
        String oldRawPass = cmd.getOldPassword();
        String newRawPass = cmd.getNewPassword();

        String hashedOld = authService.hashPasswordSHA256(oldRawPass);
        String hashedNew = authService.hashPasswordSHA256(newRawPass);

        if (!user.getPassword().equals(hashedOld)) {
            view.showError(SystemMessage.PROFILE_WRONG_OLD_PASSWORD.getMessage());
        } else if (user.getPassword().equals(hashedNew)) {
            view.showError(SystemMessage.PROFILE_SAME_PASSWORD.getMessage());
        } else {
            SystemMessage passErr = AuthValidator.getPasswordWeaknessReason(newRawPass);
            if (passErr != null) {
                view.showError(passErr.getMessage());
            } else {
                user.setPassword(hashedNew);
                userManager.save();
                view.showSuccess(SystemMessage.PROFILE_PASSWORD_CHANGED.getMessage());
            }
        }
    }

    private void handleShowInfo(User user) {
        String info = "Username: " + user.getUsername() + "\n" +
                "Nickname: " + user.getNickname() + "\n" +
                "Games Played: " + user.getGamesPlayed() + "\n" +
                "Coins: " + user.getCoins() + "\n" +
                "Gems: " + user.getDiamonds() + "\n" +
                "Passed Levels: " + user.getClearedStages() + "\n" +
                "Highest Score: " + user.getMaxMewPoint();
        view.showMessage(info);
    }
}