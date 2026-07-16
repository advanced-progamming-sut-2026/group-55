package pvz.controller.game;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GameCommand {
    PLANT("^plant\\s+plant\\s+-t\\s+(?<type>.+?)\\s+-l\\s+\\((?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\)$"),
    ADVANCE_TIME("^advance\\s+time\\s+-t\\s+(?<count>\\d+)\\s+ticks?$"),
    SHOW_MAP("^show\\s+map$");

    private final Pattern pattern;

    GameCommand(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    public Matcher getMatcher(String input) {
        Matcher matcher = pattern.matcher(input.trim());
        return matcher.matches() ? matcher : null;
    }
}