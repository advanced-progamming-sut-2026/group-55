package pvz.model.command;

public class ShopCommand implements Command {

    public enum Action {
        SHOW_LIST,
        SHOW_DAILY,
        BUY
    }

    private final Action action;
    private final int itemId;
    private final int count;
    private final String plantType;

    public ShopCommand(Action action) {
        this.action = action;
        this.itemId = -1;
        this.count = -1;
        this.plantType = null;
    }

    public ShopCommand(Action action, int itemId, int count, String plantType) {
        this.action = action;
        this.itemId = itemId;
        this.count = count;
        this.plantType = plantType;
    }

    public Action getAction() { return action; }
    public int getItemId() { return itemId; }
    public int getCount() { return count; }
    public String getPlantType() { return plantType; }
}