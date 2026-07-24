package pvz.model.command;

public class GreenhouseCommand implements Command {

    public enum Action {
        SHOW,
        PLANT,
        COLLECT,
        GROW,
        UNLOCK
    }

    private final Action action;
    private final int x;
    private final int y;

    private GreenhouseCommand(Action action, int x, int y) {
        this.action = action;
        this.x = x;
        this.y = y;
    }

    public static GreenhouseCommand createShow() {
        return new GreenhouseCommand(Action.SHOW, -1, -1);
    }

    public static GreenhouseCommand createPlant(int x, int y) {
        return new GreenhouseCommand(Action.PLANT, x, y);
    }

    public static GreenhouseCommand createCollect(int x, int y) {
        return new GreenhouseCommand(Action.COLLECT, x, y);
    }

    public static GreenhouseCommand createGrow(int x, int y) {
        return new GreenhouseCommand(Action.GROW, x, y);
    }

    public static GreenhouseCommand createUnlock(int x, int y) {
        return new GreenhouseCommand(Action.UNLOCK, x, y);
    }

    public Action getAction() { return action; }
    public int getX() { return x; }
    public int getY() { return y; }
}