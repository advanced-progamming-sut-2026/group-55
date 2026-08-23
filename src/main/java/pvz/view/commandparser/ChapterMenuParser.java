package pvz.view.commandparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pvz.model.command.ChapterCommand;
import pvz.model.command.Command;

public final class ChapterMenuParser {
    private static final Pattern ENTER_LEVEL = Pattern.compile(
            "^(?:select|enter) level -l (?<level>.+)$"
    );

    public Command parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.equals("show levels")) {
            return ChapterCommand.showLevels();
        }
        Matcher matcher = ENTER_LEVEL.matcher(trimmed);
        if (matcher.matches()) {
            return ChapterCommand.enterLevel(matcher.group("level"));
        }
        return null;
    }
}
