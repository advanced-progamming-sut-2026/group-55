package pvz.model.command;

public class CollectionCommand implements Command {

    public enum Action {
        SHOW_PLANTS,
        SHOW_ALL_PLANTS,
        SHOW_ZOMBIES,
        SHOW_ALL_ZOMBIES,
        SHOW_PLANT_DETAILS,
        SHOW_ZOMBIE_DETAILS,
        UPGRADE_PLANT,
        PURCHASE_PLANT
    }

    private final Action action;
    private final String targetName;

    public CollectionCommand(Action action) {
        this.action = action;
        this.targetName = null;
    }

    public CollectionCommand(Action action, String targetName) {
        this.action = action;
        this.targetName = targetName;
    }

    public Action getAction() {
        return action;
    }

    public String getTargetName() {
        return targetName;
    }
}