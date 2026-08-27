package pvz.model.command;

public class SettingsCommand implements Command {
    public enum Action {
        CHANGE_DIFFICULTY,
        CHANGE_GAME_SPEED,
        TOGGLE_GRID,
        TOGGLE_DEBUG
    }

    private final Action action;
    private final int level;
    private final boolean enabled;

    private SettingsCommand(Action action,int level,boolean enabled){
        this.action=action;
        this.level=level;
        this.enabled=enabled;
    }

    public static SettingsCommand createChangeDifficulty(int level){
        return new SettingsCommand(Action.CHANGE_DIFFICULTY,level,false);
    }

    public static SettingsCommand createChangeGameSpeed(int speed){
        return new SettingsCommand(Action.CHANGE_GAME_SPEED,speed,false);
    }

    public static SettingsCommand createToggleGrid(boolean enabled){
        return new SettingsCommand(Action.TOGGLE_GRID,0,enabled);
    }

    public static SettingsCommand createToggleDebug(boolean enabled){
        return new SettingsCommand(Action.TOGGLE_DEBUG,0,enabled);
    }

    public Action getAction(){return action;}
    public int getLevel(){return level;}
    public boolean isEnabled(){return enabled;}
}
