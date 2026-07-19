package pvz.view.commandparser;

import pvz.model.Command.Command;
import pvz.model.Command.ProfileCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileParser {

    private final Pattern changeUsernamePattern = Pattern.compile("^menu profile change-username -u (?<username>\\S+)$");
    private final Pattern changeNicknamePattern = Pattern.compile("^menu profile change-nickname -n (?<nickname>\\S+)$");
    private final Pattern changeEmailPattern = Pattern.compile("^menu profile change-email -e (?<email>\\S+)$");
    private final Pattern changePasswordPattern = Pattern.compile("^menu profile change-password -p (?<newPass>\\S+) -o (?<oldPass>\\S+)$");

    public Command parse(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String trimmed = input.trim();

        Matcher usernameMatcher = changeUsernamePattern.matcher(trimmed);
        if (usernameMatcher.matches()) {
            return ProfileCommand.createChangeUsername(usernameMatcher.group("username"));
        }

        Matcher nicknameMatcher = changeNicknamePattern.matcher(trimmed);
        if (nicknameMatcher.matches()) {
            return ProfileCommand.createChangeNickname(nicknameMatcher.group("nickname"));
        }

        Matcher emailMatcher = changeEmailPattern.matcher(trimmed);
        if (emailMatcher.matches()) {
            return ProfileCommand.createChangeEmail(emailMatcher.group("email"));
        }

        Matcher passwordMatcher = changePasswordPattern.matcher(trimmed);
        if (passwordMatcher.matches()) {
            return ProfileCommand.createChangePassword(passwordMatcher.group("newPass"), passwordMatcher.group("oldPass"));
        }

        if (trimmed.equals("menu profile show-info")) {
            return ProfileCommand.createShowInfo();
        }

        return null;
    }
}