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
        if (input == null || input.trim().isEmpty()) return null;
        String trimmed = input.trim();

        Matcher showPlantMatcher = showPlantPattern.matcher(trimmed);
        if (showPlantMatcher.matches()) {
            return new CollectionCommand(CollectionCommand.Action.SHOW_PLANT_DETAILS, showPlantMatcher.group("plantname"));
        }

        Matcher showZombieMatcher = showZombiePattern.matcher(trimmed);
        if (showZombieMatcher.matches()) {
            return new CollectionCommand(CollectionCommand.Action.SHOW_ZOMBIE_DETAILS, showZombieMatcher.group("zombiename"));
        }

        Matcher purchaseMatcher = purchasePlantPattern.matcher(trimmed);
        if (purchaseMatcher.matches()) {
            return new CollectionCommand(CollectionCommand.Action.PURCHASE_PLANT, purchaseMatcher.group("plantname"));
        }

        Matcher upgradeMatcher = upgradePlantPattern.matcher(trimmed);
        if (upgradeMatcher.matches()) {
            return new CollectionCommand(CollectionCommand.Action.UPGRADE_PLANT, upgradeMatcher.group("plantname"));
        }

        return switch (trimmed) {
            case "menu collection show-plants" -> new CollectionCommand(CollectionCommand.Action.SHOW_PLANTS);
            case "menu collection show-all-plants" -> new CollectionCommand(CollectionCommand.Action.SHOW_ALL_PLANTS);
            case "menu collection show-zombies" -> new CollectionCommand(CollectionCommand.Action.SHOW_ZOMBIES);
            case "menu collection show-all-zombies" -> new CollectionCommand(CollectionCommand.Action.SHOW_ALL_ZOMBIES);
            default -> null;
        };
    }
}