package pvz.model.command;

public final class ChapterCommand implements Command {
    public enum Action {
        SHOW_LEVELS,
        ENTER_LEVEL
    }

    private final Action action;
    private final String level;

    private ChapterCommand(Action action, String level) {
        this.action = action;
        this.level = level;
    }

    public static ChapterCommand showLevels() {
        return new ChapterCommand(Action.SHOW_LEVELS, null);
    }

    public static ChapterCommand enterLevel(String level) {
        return new ChapterCommand(Action.ENTER_LEVEL, level);
    }

    public Action getAction() {
        return action;
    }

    public String getLevel() {
        return level;
    }
}
