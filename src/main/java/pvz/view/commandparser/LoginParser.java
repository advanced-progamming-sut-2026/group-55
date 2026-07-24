package pvz.view.commandparser;

import pvz.model.command.Command;
import pvz.model.command.LoginCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginParser {

    private final Pattern loginPattern = Pattern.compile(
            "^login\\s+-u\\s+(?<username>\\S+)\\s+-p\\s+(?<password>\\S+)(?<stayLoggedIn>\\s+-stay-logged-in)?$");
    private final Pattern forgetPattern = Pattern.compile("^forget password -u (?<username>\\S+) -e (?<email>\\S+)$");
    private final Pattern answerPattern = Pattern.compile("^answer -a (?<answer>\\S+)$");

    public Command parse(String input) {
        if (input == null || input.isBlank()) return new Command.EmptyCommand();
        String trimmed = input.trim();

        Command regexCmd = tryParseRegexCommands(trimmed);
        if (regexCmd != null) return regexCmd;

        return new Command.RawTextCommand(trimmed);
    }

    private Command tryParseRegexCommands(String input) {
        Matcher loginMatcher = loginPattern.matcher(input);
        if (loginMatcher.matches()) {
            return LoginCommand.createLogin(
                    loginMatcher.group("username"),
                    loginMatcher.group("password"),
                    loginMatcher.group("stayLoggedIn") != null
            );
        }

        Matcher forgetMatcher = forgetPattern.matcher(input);
        if (forgetMatcher.matches()) {
            return LoginCommand.createForgetPassword(forgetMatcher.group("username"), forgetMatcher.group("email"));
        }

        Matcher answerMatcher = answerPattern.matcher(input);
        if (answerMatcher.matches()) {
            return LoginCommand.createAnswer(answerMatcher.group("answer"));
        }

        return null;
    }
}