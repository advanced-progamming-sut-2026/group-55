package pvz.model.Command;

public class ProfileCommand implements Command {

    public enum Action {
        SHOW_INFO,
        CHANGE_USERNAME,
        CHANGE_NICKNAME,
        CHANGE_EMAIL,
        CHANGE_PASSWORD
    }

    private final Action action;
    private final String newUsername;
    private final String newNickname;
    private final String newEmail;
    private final String newPassword;
    private final String oldPassword;

    private ProfileCommand(Action action, String newUsername, String newNickname,
                           String newEmail, String newPassword, String oldPassword) {
        this.action = action;
        this.newUsername = newUsername;
        this.newNickname = newNickname;
        this.newEmail = newEmail;
        this.newPassword = newPassword;
        this.oldPassword = oldPassword;
    }

    public static ProfileCommand createShowInfo() {
        return new ProfileCommand(Action.SHOW_INFO, null, null, null, null, null);
    }

    public static ProfileCommand createChangeUsername(String newUsername) {
        return new ProfileCommand(Action.CHANGE_USERNAME, newUsername, null, null, null, null);
    }

    public static ProfileCommand createChangeNickname(String newNickname) {
        return new ProfileCommand(Action.CHANGE_NICKNAME, null, newNickname, null, null, null);
    }

    public static ProfileCommand createChangeEmail(String newEmail) {
        return new ProfileCommand(Action.CHANGE_EMAIL, null, null, newEmail, null, null);
    }

    public static ProfileCommand createChangePassword(String newPassword, String oldPassword) {
        return new ProfileCommand(Action.CHANGE_PASSWORD, null, null, null, newPassword, oldPassword);
    }

    public Action getAction() { return action; }
    public String getNewUsername() { return newUsername; }
    public String getNewNickname() { return newNickname; }
    public String getNewEmail() { return newEmail; }
    public String getNewPassword() { return newPassword; }
    public String getOldPassword() { return oldPassword; }
}