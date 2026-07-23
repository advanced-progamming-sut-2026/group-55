package pvz.view.commandparser;

import pvz.model.command.Command;
import pvz.model.command.GameMenuCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameMenuParser {

    private final Pattern enterChapterPattern = Pattern.compile("^menu enter chapter -c (?<chaptername>.+)$");
    private final Pattern cheatAddPattern = Pattern.compile("^menu cheat add (?<amount>\\d+) (?<type>coin|diamond)$");
    private final Pattern changeWorldPattern = Pattern.compile("^menu world (?<worldname>.+)$");

    public Command parse(String input) {
        if (input == null || input.isBlank()) return null;
        String trimmed = input.trim();

        Command regexCmd = tryParseRegexCommands(trimmed);
        if (regexCmd != null) return regexCmd;

        return tryParseFixedCommands(trimmed);
    }

    private Command tryParseRegexCommands(String input) {
        Matcher enterChapter = enterChapterPattern.matcher(input);
        if (enterChapter.matches()) return GameMenuCommand.createEnterChapter(enterChapter.group("chaptername"));

        Matcher cheat = cheatAddPattern.matcher(input);
        if (cheat.matches()) {
            return GameMenuCommand.createCheatAdd(
                    Integer.parseInt(cheat.group("amount")),
                    cheat.group("type"));
        }

        Matcher world = changeWorldPattern.matcher(input);
        if (world.matches()) return GameMenuCommand.createChangeWorld(world.group("worldname"));

        return null;
    }

    private Command tryParseFixedCommands(String input) {
        return switch (input) {
            case "menu enter collection" -> GameMenuCommand.createEnterCollection();
            case "menu greenhouse" -> GameMenuCommand.createGreenhouse();
            case "menu travel-log" -> GameMenuCommand.createTravelLog();
            case "menu leaderboard" -> GameMenuCommand.createLeaderboard();
            case "menu coin-wallet" -> GameMenuCommand.createCoinWallet();
            case "menu gem-wallet" -> GameMenuCommand.createGemWallet();
            default -> null;
        };
    }
}