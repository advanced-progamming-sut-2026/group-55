package pvz.model.service;

import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.greenhouse.Pot;
import pvz.model.shop.DailyOffer;
import pvz.model.shop.ShopData;
import pvz.model.shop.ShopItem;
import pvz.model.utils.SystemMessage;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

public class ShopService {

    private final Random random = new Random();
    private static final int DAILY_OFFER_ID = 6;
    private static final int DAILY_OFFER_PRICE = 1600;

    public void buy(User user, int itemId, int count, String plantType) throws Exception {
        if (count <= 0) throw new Exception(SystemMessage.SHOP_INVALID_COUNT.getMessage());

        if (itemId == DAILY_OFFER_ID) {
            buyDailyOffer(user, count);
            return;
        }

        ShopItem item = ShopData.getItemById(itemId);
        if (item == null) throw new Exception(SystemMessage.SHOP_INVALID_ITEM_ID.getMessage());

        int totalCoin = item.getCoinPrice() * count;
        int totalDiamond = item.getDiamondPrice() * count;

        switch (item.getType()) {
            case POT -> buyPot(user, count, totalCoin);
            case PLANT_FOOD -> buyPlantFood(user, count, totalDiamond);
            case RANDOM_SEED -> buyRandomSeed(user, count, totalCoin);
            case SELECT_SEED -> buySelectedSeed(user, count, totalDiamond, plantType);
            case DIAMOND_TO_COIN -> buyDiamondExchange(user, count, totalDiamond);
            default -> throw new Exception(SystemMessage.SHOP_UNKNOWN_ITEM_TYPE.getMessage());
        }
    }

    private void buyPot(User user, int count, int totalCoin) throws Exception {
        int lockedPots = 0;
        for (int y = 2; y <= 4; y++) {
            for (int x = 1; x <= 5; x++) {
                Pot pot = user.getGreenhouse().getPot(x, y);
                if (pot != null && pot.isLocked()) lockedPots++;
            }
        }

        if (count > lockedPots) {
            throw new Exception(SystemMessage.SHOP_POTS_MAX_CAPACITY.getMessage());
        }
        if (!user.spendCoins(totalCoin)) {
            throw new Exception(SystemMessage.SHOP_NOT_ENOUGH_COINS.getMessage());
        }

        for (int i = 0; i < count; i++) {
            user.getGreenhouse().unlockNextAvailablePot();
        }
    }

    private void buyPlantFood(User user, int count, int totalDiamond) throws Exception {
        if (user.getPlantFoodCount() + count > 3) {
            throw new Exception(SystemMessage.SHOP_PLANT_FOOD_MAX_CAPACITY.getMessage());
        }
        if (!user.spendDiamonds(totalDiamond)) {
            throw new Exception(SystemMessage.SHOP_NOT_ENOUGH_DIAMONDS.getMessage());
        }
        user.addPlantFood(count);
    }

    private void buyRandomSeed(User user, int count, int totalCoin) throws Exception {
        List<PlayerPlant> unlocked = user.getUnlockedPlants();
        if (unlocked.isEmpty()) {
            throw new Exception(SystemMessage.SHOP_NO_UNLOCKED_PLANTS.getMessage());
        }
        if (!user.spendCoins(totalCoin)) {
            throw new Exception(SystemMessage.SHOP_NOT_ENOUGH_COINS.getMessage());
        }

        for (int i = 0; i < count; i++) {
            PlayerPlant randomPlant = unlocked.get(random.nextInt(unlocked.size()));
            randomPlant.addSeedPackets(5);
        }
    }

    private void buySelectedSeed(User user, int count, int totalDiamond, String plantType) throws Exception {
        if (plantType == null || plantType.trim().isEmpty()) {
            throw new Exception(SystemMessage.SHOP_PLANT_TYPE_REQUIRED.getMessage());
        }

        PlayerPlant targetPlant = user.getOwnedPlant(plantType);
        if (targetPlant == null) {
            throw new Exception(SystemMessage.SHOP_PLANT_NOT_UNLOCKED.getMessage());
        }

        if (!user.spendDiamonds(totalDiamond)) {
            throw new Exception(SystemMessage.SHOP_NOT_ENOUGH_DIAMONDS.getMessage());
        }

        targetPlant.addSeedPackets(count * 10);
    }

    private void buyDiamondExchange(User user, int count, int totalDiamond) throws Exception {
        if (!user.spendDiamonds(totalDiamond)) {
            throw new Exception(SystemMessage.SHOP_NOT_ENOUGH_DIAMONDS.getMessage());
        }
        user.addCoins(count * 500);
    }

    private void buyDailyOffer(User user, int count) throws Exception {
        if (count > 1) {
            throw new Exception(SystemMessage.SHOP_DAILY_OFFER_ONCE.getMessage());
        }

        DailyOffer offer = getOrGenerateDailyOffer(user);
        if (offer.isPurchased()) {
            throw new Exception(SystemMessage.SHOP_DAILY_OFFER_ALREADY_BOUGHT.getMessage());
        }

        if (!user.spendCoins(offer.getPrice())) {
            throw new Exception(SystemMessage.SHOP_NOT_ENOUGH_COINS.getMessage());
        }

        PlayerPlant targetPlant = user.getOwnedPlant(offer.getPlantName());
        if (targetPlant != null) targetPlant.addSeedPackets(10);

        offer.setPurchased(true);
    }

    public DailyOffer getOrGenerateDailyOffer(User user) throws Exception {
        DailyOffer currentOffer = user.getDailyOffer();
        LocalDate today = LocalDate.now();

        if (currentOffer == null || !currentOffer.getDate().equals(today)) {
            List<PlayerPlant> unlocked = user.getUnlockedPlants();
            if (unlocked.isEmpty()) {
                throw new Exception(SystemMessage.SHOP_DAILY_OFFER_NO_PLANTS.getMessage());
            }

            PlayerPlant randomPlant = unlocked.get(random.nextInt(unlocked.size()));
            currentOffer = new DailyOffer(randomPlant.getPlantName(), DAILY_OFFER_PRICE, today);
            user.setDailyOffer(currentOffer);
        }

        return currentOffer;
    }
}