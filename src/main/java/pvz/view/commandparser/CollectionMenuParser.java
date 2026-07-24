package pvz.view.commandparser;

import pvz.model.command.CollectionCommand;
import pvz.model.command.Command;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CollectionMenuParser {

    private final Pattern showPlantPattern = Pattern.compile("^menu collection show-plant -p (?<plantname>.+)$");
    private final Pattern showZombiePattern = Pattern.compile("^menu collection show-zombie -z (?<zombiename>.+)$");
    private final Pattern purchasePlantPattern = Pattern.compile("^menu collection purchase-plant -p (?<plantname>.+)$");
    private final Pattern upgradePlantPattern = Pattern.compile("^menu collection upgrade-plant -p (?<plantname>.+)$");

    public Command parse(String input) {
        if (input == null || input.isBlank()) return null;
        String trimmed = input.trim();

        Command regexCommand = tryParseRegexCommands(trimmed);
        if (regexCommand != null) return regexCommand;

        return tryParseFixedCommands(trimmed);
    }

    private Command tryParseRegexCommands(String input) {
        Matcher showPlant = showPlantPattern.matcher(input);
        if (showPlant.matches()) return new CollectionCommand(CollectionCommand.Action.SHOW_PLANT_DETAILS, showPlant.group("plantname"));

        Matcher showZombie = showZombiePattern.matcher(input);
        if (showZombie.matches()) return new CollectionCommand(CollectionCommand.Action.SHOW_ZOMBIE_DETAILS, showZombie.group("zombiename"));

        Matcher purchase = purchasePlantPattern.matcher(input);
        if (purchase.matches()) return new CollectionCommand(CollectionCommand.Action.PURCHASE_PLANT, purchase.group("plantname"));

        Matcher upgrade = upgradePlantPattern.matcher(input);
        if (upgrade.matches()) return new CollectionCommand(CollectionCommand.Action.UPGRADE_PLANT, upgrade.group("plantname"));

        return null;
    }

    private Command tryParseFixedCommands(String input) {
        return switch (input) {
            case "menu collection show-plants" -> new CollectionCommand(CollectionCommand.Action.SHOW_PLANTS);
            case "menu collection show-all-plants" -> new CollectionCommand(CollectionCommand.Action.SHOW_ALL_PLANTS);
            case "menu collection show-zombies" -> new CollectionCommand(CollectionCommand.Action.SHOW_ZOMBIES);
            case "menu collection show-all-zombies" -> new CollectionCommand(CollectionCommand.Action.SHOW_ALL_ZOMBIES);
            default -> null;
        };
    }
}