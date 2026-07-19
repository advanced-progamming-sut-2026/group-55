package pvz.view.commandparser;

import pvz.model.Command.RegisterCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterParser {

    private final Pattern registerPattern = Pattern.compile("^register -u (?<username>\\S+) -p (?<password>\\S+) " +
            "(?<passwordConfirm>\\S+) -n (?<nickname>\\S+) -e (?<email>\\S+) -g (?<gender>\\S+)$");

    private final Pattern pickQuestionPattern = Pattern.compile("^pick question -q (?<questionNumber>\\d+) " +
            "-a (?<answer>\\S+) -c (?<answerConfirm>\\S+)$");

    public RegisterCommand parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim();

        Matcher registerMatcher = registerPattern.matcher(trimmed);
        if (registerMatcher.matches()) {
            return RegisterCommand.createRegister(
                    registerMatcher.group("username"),
                    registerMatcher.group("password"),
                    registerMatcher.group("passwordConfirm"),
                    registerMatcher.group("nickname"),
                    registerMatcher.group("email"),
                    registerMatcher.group("gender")
            );
        }

        Matcher pickQuestionMatcher = pickQuestionPattern.matcher(trimmed);
        if (pickQuestionMatcher.matches()) {
            return RegisterCommand.createPickQuestion(
                    Integer.parseInt(pickQuestionMatcher.group("questionNumber")),
                    pickQuestionMatcher.group("answer"),
                    pickQuestionMatcher.group("answerConfirm")
            );
        }

        return null;
    }
}