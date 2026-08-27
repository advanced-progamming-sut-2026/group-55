package pvz.view.commandparser;

import pvz.model.command.Command;
import pvz.model.command.SettingsCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsParser {
    private final Pattern changeDifficultyPattern=Pattern.compile("^menu settings change-difficulty -l (?<level>\\d+)$");
    private final Pattern changeSpeedPattern=Pattern.compile("^menu settings change-speed -s (?<speed>\\d+)$");
    private final Pattern gridPattern=Pattern.compile("^menu settings grid (?<state>true|false)$");
    private final Pattern debugPattern=Pattern.compile("^menu settings debug (?<state>true|false)$");

    public Command parse(String input){
        if(input==null||input.isBlank()) return null;
        String trimmed=input.trim();

        Matcher matcher=changeDifficultyPattern.matcher(trimmed);
        if(matcher.matches()) return SettingsCommand.createChangeDifficulty(Integer.parseInt(matcher.group("level")));

        matcher=changeSpeedPattern.matcher(trimmed);
        if(matcher.matches()) return SettingsCommand.createChangeGameSpeed(Integer.parseInt(matcher.group("speed")));

        matcher=gridPattern.matcher(trimmed);
        if(matcher.matches()) return SettingsCommand.createToggleGrid(Boolean.parseBoolean(matcher.group("state")));

        matcher=debugPattern.matcher(trimmed);
        if(matcher.matches()) return SettingsCommand.createToggleDebug(Boolean.parseBoolean(matcher.group("state")));

        return null;
    }
}
