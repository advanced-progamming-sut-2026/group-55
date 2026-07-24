package pvz.view.commandparser;

import pvz.model.command.Command;
import pvz.model.command.PlantSelectionCommand;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlantSelectionMenuParser {

    private final Pattern addPlantPattern = Pattern.compile("^add plant -t (?<plantname>.+)$");
    private final Pattern removePlantPattern = Pattern.compile("^remove plant -t (?<plantname>.+)$");
    private final Pattern boostPlantPattern = Pattern.compile("^boost plant -t (?<plantname>.+)$");

    public Command parse(String input) {
        if (input == null || input.isBlank()) return null;
        String trimmed = input.trim();

        Command regexCmd = tryParseRegexCommands(trimmed);
        if (regexCmd != null) return regexCmd;

        return tryParseFixedCommands(trimmed);
    }

    private Command tryParseRegexCommands(String input) {
        Matcher addMatcher = addPlantPattern.matcher(input);
        if (addMatcher.matches()) {
            return new PlantSelectionCommand(PlantSelectionCommand.Action.ADD_PLANT, addMatcher.group("plantname"));
        }

        Matcher removeMatcher = removePlantPattern.matcher(input);
        if (removeMatcher.matches()) {
            return new PlantSelectionCommand(PlantSelectionCommand.Action.REMOVE_PLANT, removeMatcher.group("plantname"));
        }

        Matcher boostMatcher = boostPlantPattern.matcher(input);
        if (boostMatcher.matches()) {
            return new PlantSelectionCommand(PlantSelectionCommand.Action.BOOST_PLANT, boostMatcher.group("plantname"));
        }

        return null;
    }

    private Command tryParseFixedCommands(String input) {
        return switch (input) {
            case "show all plants" -> new PlantSelectionCommand(PlantSelectionCommand.Action.SHOW_ALL_PLANTS);
            case "show available plants" -> new PlantSelectionCommand(PlantSelectionCommand.Action.SHOW_AVAILABLE_PLANTS);
            case "show selected plants" -> new PlantSelectionCommand(PlantSelectionCommand.Action.SHOW_SELECTED_PLANTS);
            case "start game" -> new PlantSelectionCommand(PlantSelectionCommand.Action.START_GAME);
            default -> null;
        };
    }
}