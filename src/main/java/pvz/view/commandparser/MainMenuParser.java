package pvz.view.commandparser;

import pvz.model.command.Command;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainMenuParser {

    private final Pattern menuEnterPattern = Pattern.compile("^menu enter\\s+(?<menuName>.+)$");

    public Command parse(String input) {
        if (input == null || input.isBlank()) return new Command.EmptyCommand();
        String trimmed = input.trim();

        if (trimmed.equals("play")) return new Command.MenuEnterCommand("game");

        Command regexCmd = tryParseRegexCommands(trimmed);
        if (regexCmd != null) return regexCmd;

        return tryParseFixedCommands(trimmed);
    }

    private Command tryParseRegexCommands(String input) {
        Matcher menuEnterMatcher = menuEnterPattern.matcher(input);
        if (menuEnterMatcher.matches()) {
            return new Command.MenuEnterCommand(menuEnterMatcher.group("menuName"));
        }
        return null;
    }

    private Command tryParseFixedCommands(String input) {
        return switch (input) {
            case "menu show current" -> new Command.MenuShowCurrentCommand();
            case "menu exit" -> new Command.MenuExitCommand();
            case "menu logout" -> new Command.MenuLogoutCommand();
            default -> new Command.RawTextCommand(input);
        };
    }
}