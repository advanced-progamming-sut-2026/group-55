package pvz.view.commandparser;

import pvz.model.Command.Command;
import pvz.model.Command.SettingsCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsParser {

    private final Pattern changeDifficultyPattern = Pattern.compile("^menu settings change-difficulty -l (?<level>\\d+)$");

    public Command parse(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String trimmed = input.trim();

        Matcher difficultyMatcher = changeDifficultyPattern.matcher(trimmed);
        if (difficultyMatcher.matches()) {
            return SettingsCommand.createChangeDifficulty(Integer.parseInt(difficultyMatcher.group("level")));
        }

        return null;
    }
}