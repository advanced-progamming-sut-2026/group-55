package pvz.controller.game;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GameCommand {
    PLANT("^plant\\s+plant\\s+-t\\s+(?<type>.+?)\\s+-l\\s+\\((?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\)$"),
    ADVANCE_TIME("^advance\\s+time\\s+-t\\s+(?<count>\\d+)\\s+ticks?$"),
    SHOW_MAP("^show\\s+map$"),
    PLUCK("^pluck\\s+plant\\s+-l\\s+\\((?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\)$"),
    SHOW_SUN("^show\\s+sun\\s+amount$"),
    COLLECT_SUN("^collect\\s+sun\\s+-l\\s+\\((?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\)$"),
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
