package pvz.view.commandparser;

import pvz.model.command.Command;
import pvz.model.command.GreenhouseCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GreenhouseParser {

    private final Pattern plantPattern = Pattern.compile("^plant pot at \\((?<x>\\d+),\\s*(?<y>\\d+)\\)$");
    private final Pattern collectPattern = Pattern.compile("^collect \\((?<x>\\d+),\\s*(?<y>\\d+)\\)$");
    private final Pattern growPattern = Pattern.compile("^grow \\((?<x>\\d+),\\s*(?<y>\\d+)\\)$");
    private final Pattern unlockPattern = Pattern.compile("^unlock pot at \\((?<x>\\d+),\\s*(?<y>\\d+)\\)$");
    private final Pattern enterPattern = Pattern.compile("^enter\\s+(?<menuName>.+)$");

    public Command parse(String input) {
        if (input == null || input.isBlank()) return null;
        String trimmed = input.trim();

        if (trimmed.equals("show greenhouse")) {
            return GreenhouseCommand.createShow();
        }

        Matcher enterMatcher = enterPattern.matcher(trimmed);
        if (enterMatcher.matches()) {
            return new Command.MenuEnterCommand(enterMatcher.group("menuName"));
        }

        Matcher plantMatcher = plantPattern.matcher(trimmed);
        if (plantMatcher.matches()) {
            return GreenhouseCommand.createPlant(
                    Integer.parseInt(plantMatcher.group("x")),
                    Integer.parseInt(plantMatcher.group("y"))
            );
        }

        Matcher collectMatcher = collectPattern.matcher(trimmed);
        if (collectMatcher.matches()) {
            return GreenhouseCommand.createCollect(
                    Integer.parseInt(collectMatcher.group("x")),
                    Integer.parseInt(collectMatcher.group("y"))
            );
        }

        Matcher growMatcher = growPattern.matcher(trimmed);
        if (growMatcher.matches()) {
            return GreenhouseCommand.createGrow(
                    Integer.parseInt(growMatcher.group("x")),
                    Integer.parseInt(growMatcher.group("y"))
            );
        }

        Matcher unlockMatcher = unlockPattern.matcher(trimmed);
        if (unlockMatcher.matches()) {
            return GreenhouseCommand.createUnlock(
                    Integer.parseInt(unlockMatcher.group("x")),
                    Integer.parseInt(unlockMatcher.group("y"))
            );
        }

        return null;
    }
}