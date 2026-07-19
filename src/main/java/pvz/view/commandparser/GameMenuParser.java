package pvz.view.commandparser;

import pvz.model.Command.Command;
import pvz.model.Command.GameMenuCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameMenuParser {

    private final Pattern enterChapterPattern = Pattern.compile("^menu enter chapter c (?<chaptername>.+)$");
    private final Pattern cheatAddPattern = Pattern.compile("^menu cheat add (?<amount>\\d+) (?<type>coin|diamond)$");
    private final Pattern changeWorldPattern = Pattern.compile("^menu world (?<worldname>.+)$");

    public Command parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim();

        Matcher enterChapterMatcher = enterChapterPattern.matcher(trimmed);
        if (enterChapterMatcher.matches()) {
            return GameMenuCommand.createEnterChapter(enterChapterMatcher.group("chaptername"));
        }

        Matcher cheatMatcher = cheatAddPattern.matcher(trimmed);
        if (cheatMatcher.matches()) {
            return GameMenuCommand.createCheatAdd(
                    Integer.parseInt(cheatMatcher.group("amount")),
                    cheatMatcher.group("type"));
        }

        Matcher worldMatcher = changeWorldPattern.matcher(trimmed);
        if (worldMatcher.matches()) {
            return GameMenuCommand.createChangeWorld(worldMatcher.group("worldname"));
        }

        return switch (trimmed) {
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