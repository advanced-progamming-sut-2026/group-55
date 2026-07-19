package pvz.model.Command;

public class SettingsCommand implements Command {

    public enum Action {
        CHANGE_DIFFICULTY
    }

    private final Action action;
    private final int level;

    private SettingsCommand(Action action, int level) {
        this.action = action;
        this.level = level;
    }

    public static SettingsCommand createChangeDifficulty(int level) {
        return new SettingsCommand(Action.CHANGE_DIFFICULTY, level);
    }

    public Action getAction() { return action; }
    public int getLevel() { return level; }
}