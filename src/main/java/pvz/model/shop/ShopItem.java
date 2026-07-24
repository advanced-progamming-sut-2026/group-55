package pvz.model.shop;

public class ShopItem {
    private final int id;
    private final String name;
    private final int coinPrice;
    private final int diamondPrice;
    private final int unit;
    private final ShopItemType type;

    public ShopItem(int id, String name, int coinPrice, int diamondPrice, int unit, ShopItemType type) {
        this.id = id;
        this.name = name;
        this.coinPrice = coinPrice;
        this.diamondPrice = diamondPrice;
        this.unit = unit;
        this.type = type;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getCoinPrice() { return coinPrice; }
    public int getDiamondPrice() { return diamondPrice; }
    public int getUnit() { return unit; }
    public ShopItemType getType() { return type; }
}