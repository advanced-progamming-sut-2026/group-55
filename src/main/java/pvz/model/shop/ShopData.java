package pvz.model.shop;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class ShopData {
    private static final Map<Integer, ShopItem> ITEMS = new HashMap<>();

    static {
        ITEMS.put(1, new ShopItem(1, "Pot", 2000, 0, 1, ShopItemType.POT));
        ITEMS.put(2, new ShopItem(2, "Plant Food", 0, 3, 1, ShopItemType.PLANT_FOOD));
        ITEMS.put(3, new ShopItem(3, "Random Seed", 1000, 0, 5, ShopItemType.RANDOM_SEED));
        ITEMS.put(4, new ShopItem(4, "Selected Seed", 0, 5, 10, ShopItemType.SELECT_SEED));
        ITEMS.put(5, new ShopItem(5, "Diamond Exchange", 0, 5, 500, ShopItemType.DIAMOND_TO_COIN));
    }

    private ShopData() { }

    public static ShopItem getItemById(int id) {
        return ITEMS.get(id);
    }

    public static Collection<ShopItem> getAllItems() {
        return ITEMS.values();
    }
}
