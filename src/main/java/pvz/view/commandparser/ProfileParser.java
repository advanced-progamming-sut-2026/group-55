package pvz.view.commandparser;

import pvz.model.command.Command;
import pvz.model.command.ProfileCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileParser {

    private final Pattern changeUsernamePattern = Pattern.compile("^menu profile change-username -u (?<username>\\S+)$");
    private final Pattern changeNicknamePattern = Pattern.compile("^menu profile change-nickname -n (?<nickname>.+)$");
    private final Pattern changeEmailPattern = Pattern.compile("^menu profile change-email -e (?<email>\\S+)$");
    private final Pattern changePasswordPattern = Pattern.compile("^menu profile change-password -p (?<newPass>\\S+) -o (?<oldPass>\\S+)$");

    public Command parse(String input) {
        if (input == null || input.isBlank()) return null;
        String trimmed = input.trim();

        Command regexCmd = tryParseRegexCommands(trimmed);
        if (regexCmd != null) return regexCmd;

        return tryParseFixedCommands(trimmed);
    }

    private Command tryParseRegexCommands(String input) {
        Matcher usernameMatcher = changeUsernamePattern.matcher(input);
        if (usernameMatcher.matches()) return ProfileCommand.createChangeUsername(usernameMatcher.group("username"));

        Matcher nicknameMatcher = changeNicknamePattern.matcher(input);
        if (nicknameMatcher.matches()) return ProfileCommand.createChangeNickname(nicknameMatcher.group("nickname").trim());

        Matcher emailMatcher = changeEmailPattern.matcher(input);
        if (emailMatcher.matches()) return ProfileCommand.createChangeEmail(emailMatcher.group("email"));

        Matcher passwordMatcher = changePasswordPattern.matcher(input);
        if (passwordMatcher.matches()) {
            return ProfileCommand.createChangePassword(passwordMatcher.group("newPass"), passwordMatcher.group("oldPass"));
        }

        return null;
    }

    private Command tryParseFixedCommands(String input) {
        if (input.equals("menu profile show-info")) return ProfileCommand.createShowInfo();
        return null;
    }
}