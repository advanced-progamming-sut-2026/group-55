package pvz.view.commandparser;

import pvz.model.command.Command;
import pvz.model.command.SettingsCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsParser {

    private final Pattern changeDifficultyPattern = Pattern.compile("^menu settings change-difficulty -l (?<level>\\d+)$");

    public Command parse(String input) {
        if (input == null || input.isBlank()) return null;

        return tryParseRegexCommands(input.trim());
    }

    private Command tryParseRegexCommands(String input) {
        Matcher difficultyMatcher = changeDifficultyPattern.matcher(input);
        if (difficultyMatcher.matches()) {
            return SettingsCommand.createChangeDifficulty(Integer.parseInt(difficultyMatcher.group("level")));
        }

        return null;
    }
}