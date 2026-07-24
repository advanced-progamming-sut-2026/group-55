package pvz.model.command;

public class NewsCommand implements Command {
    public enum Action {
        SHOW_UNREAD,
        SHOW_ALL
    }

    private final Action action;

    public NewsCommand(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }
}