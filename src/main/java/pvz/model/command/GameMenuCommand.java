package pvz.model.command;

public class GameMenuCommand implements Command {

    public enum Action {
        ENTER_CHAPTER,
        ENTER_COLLECTION,
        GREENHOUSE,
        TRAVEL_LOG,
        LEADERBOARD,
        COIN_WALLET,
        GEM_WALLET,
        CHANGE_WORLD,
        CHEAT_ADD
    }

    private final Action action;
    private String stringArg;
    private int intArg;

    private GameMenuCommand(Action action) {
        this.action = action;
    }

    private GameMenuCommand(Action action, String stringArg) {
        this.action = action;
        this.stringArg = stringArg;
    }

    private GameMenuCommand(Action action, int intArg, String stringArg) {
        this.action = action;
        this.intArg = intArg;
        this.stringArg = stringArg;
    }

    public static GameMenuCommand createEnterChapter(String chapterName) {
        return new GameMenuCommand(Action.ENTER_CHAPTER, chapterName);
    }

    public static GameMenuCommand createEnterCollection() {
        return new GameMenuCommand(Action.ENTER_COLLECTION);
    }

    public static GameMenuCommand createGreenhouse() {
        return new GameMenuCommand(Action.GREENHOUSE);
    }

    public static GameMenuCommand createTravelLog() {
        return new GameMenuCommand(Action.TRAVEL_LOG);
    }

    public static GameMenuCommand createLeaderboard() {
        return new GameMenuCommand(Action.LEADERBOARD);
    }

    public static GameMenuCommand createCoinWallet() {
        return new GameMenuCommand(Action.COIN_WALLET);
    }

    public static GameMenuCommand createGemWallet() {
        return new GameMenuCommand(Action.GEM_WALLET);
    }

    public static GameMenuCommand createChangeWorld(String worldName) {
        return new GameMenuCommand(Action.CHANGE_WORLD, worldName);
    }

    public static GameMenuCommand createCheatAdd(int amount, String resourceType) {
        return new GameMenuCommand(Action.CHEAT_ADD, amount, resourceType);
    }

    public Action getAction() { return action; }
    public String getStringArg() { return stringArg; }
    public int getIntArg() { return intArg; }
}