package pvz.controller.game;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GameCommand {
    PLANT("^plant\\s+plant\\s+-t\\s+(?<type>.+?)\\s+-l\\s+\\((?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\)$"),
    ADVANCE_TIME("^advance\\s+time\\s+-t\\s+(?<count>\\d+)\\s+ticks?$"),
    SHOW_MAP("^show\\s+map$"),
    SHOW_PLANTS_STATUS("^show\\s+plants\\s+status$"),
    SHOW_TILE_STATUS("^show\\s+tile\\s+status\\s+-l\\s+\\((?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\)$"),
    SHOW_ZOMBIES("^zombies\\s+info$"),
    PLUCK("^pluck\\s+plant\\s+-l\\s+\\((?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\)$"),
    SHOW_SUN("^show\\s+sun\\s+amount$"),
    COLLECT_SUN("^collect\\s+sun\\s+-l\\s+\\((?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\)$"),
    COLLECT_PLANT_FOOD("^collect\\s+plant-food\\s+-l\\s+\\((?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\)$"),
    ADD_PLANT_FOOD("^cheat\\s+add-plant-food$"),
    REMOVE_COOLDOWN("^cheat\\s+remove-cooldown$"),
    ADD_SUN("^cheat\\s+add\\s+-n\\s+(?<count>\\d+)\\s+suns?$"),
    FEED_PLANT("^feed\\s+plant\\s+-l\\s+\\((?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\)$"),
    RELEASE_NUKE("^release\\s+the\\s+nuke$"),
    SPAWN_ZOMBIE("^cheat\\s+spawn-zombie\\s+-t\\s+(?<type>\\S+)\\s+-l\\s+\\(?(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\)?$");

    private final Pattern pattern;

    GameCommand(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    public Matcher getMatcher(String input) {
        Matcher matcher = pattern.matcher(input.trim());
        return matcher.matches() ? matcher : null;
    }
}
