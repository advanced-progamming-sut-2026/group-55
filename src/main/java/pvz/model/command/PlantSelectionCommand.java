package pvz.model.command;

public class PlantSelectionCommand implements Command {

    public enum Action {
        SHOW_ALL_PLANTS,
        SHOW_AVAILABLE_PLANTS,
        SHOW_SELECTED_PLANTS,
        ADD_PLANT,
        REMOVE_PLANT,
        BOOST_PLANT,
        START_GAME
    }

    private final Action action;
    private final String targetName;

    public PlantSelectionCommand(Action action) {
        this.action = action;
        this.targetName = null;
    }

    public PlantSelectionCommand(Action action, String targetName) {
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