package pvz.view.commandparser;

import pvz.model.Command.Command;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainMenuParser {

    private final Pattern menuEnterPattern = Pattern.compile("^menu enter\\s+(?<menuName>.+)$");

    public Command parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new Command.EmptyCommand();
        }

        String trimmed = input.trim();

        Matcher menuEnterMatcher = menuEnterPattern.matcher(trimmed);
        if (menuEnterMatcher.matches()) {
            return new Command.MenuEnterCommand(menuEnterMatcher.group("menuName"));
        }

        return switch (trimmed) {
            case "menu show current" -> new Command.MenuShowCurrentCommand();
            case "menu exit" -> new Command.MenuExitCommand();
            case "menu logout" -> new Command.MenuLogoutCommand();
            default -> new Command.RawTextCommand(trimmed);
        };

    }
}