package pvz.model.service;

import pvz.model.account.PlayerPlant;
import pvz.model.account.User;
import pvz.model.shop.*;
import pvz.model.utils.SystemMessage;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

public class ShopService {

    private final Random random = new Random();
    private static final int DAILY_OFFER_ID = 6;
    private static final int DAILY_OFFER_PRICE = 1600;

    public void buy(User user, int itemId, int count, String plantType) throws Exception {
        buy(user, itemId, count, plantType, null);
    }

    public void buy(User user, int itemId, int count, String plantType,
                    DailyOffer expectedOffer) throws Exception {
        if (count <= 0) throw new Exception(SystemMessage.SHOP_INVALID_COUNT.getMessage());

        if (itemId == DAILY_OFFER_ID) {
            if (plantType != null && !plantType.trim().isEmpty()) {
                throw new Exception("Invalid command: Daily offer does not require a plant type (-t).");
            }
            buyDailyOffer(user, count, expectedOffer);
            return;
        }

        ShopItem item = ShopData.getItemById(itemId);
        if (item == null) throw new Exception(SystemMessage.SHOP_INVALID_ITEM_ID.getMessage());

        boolean hasPlantType = plantType != null && !plantType.trim().isEmpty();
        if (item.getType() != ShopItemType.SELECT_SEED && hasPlantType) {
            throw new Exception("Invalid command: This item does not require a plant type (-t).");
        }

        int totalCoin;
        int totalDiamond;
        try {
            totalCoin = Math.multiplyExact(item.getCoinPrice(), count);
            totalDiamond = Math.multiplyExact(item.getDiamondPrice(), count);
        } catch (ArithmeticException e) {
            throw new Exception("Invalid quantity resulting in overflow.");
        }

        switch (item.getType()) {
            case POT -> buyPot(user, count, totalCoin);
            case PLANT_FOOD -> buyPlantFood(user, count, totalDiamond);
            case RANDOM_SEED -> buyRandomSeed(user, count, totalCoin);
            case SELECT_SEED -> buySelectedSeed(user, count, totalDiamond, plantType);
            case DIAMOND_TO_COIN -> buyDiamondExchange(user, count, totalDiamond);
            default -> throw new Exception(SystemMessage.SHOP_UNKNOWN_ITEM_TYPE.getMessage());
        }
    }

    public void validateSinglePurchase(User user, int itemId, String plantType,
                                       DailyOffer expectedOffer) throws Exception {
        if (user == null) throw new Exception("No active user.");
        if (itemId == DAILY_OFFER_ID) {
            if (expectedOffer == null || user.getDailyOffer() != expectedOffer
                    || !expectedOffer.getDate().equals(LocalDate.now()))
                throw new Exception("Daily offer changed. Please review the new offer.");
            if (expectedOffer.isPurchased())
                throw new Exception(SystemMessage.SHOP_DAILY_OFFER_ALREADY_BOUGHT.getMessage());
            if (user.getCoins() < expectedOffer.getPrice())
                throw new Exception(SystemMessage.SHOP_NOT_ENOUGH_COINS.getMessage());
            return;
        }
        ShopItem item = ShopData.getItemById(itemId);
        if (item == null) throw new Exception(SystemMessage.SHOP_INVALID_ITEM_ID.getMessage());
        if (item.getType() == ShopItemType.POT && user.getGreenhouse().getLockedPotCount() == 0)
            throw new Exception(SystemMessage.SHOP_POTS_MAX_CAPACITY.getMessage());
        if (item.getType() == ShopItemType.PLANT_FOOD && user.getPlantFoodCount() >= 3)
            throw new Exception(SystemMessage.SHOP_PLANT_FOOD_MAX_CAPACITY.getMessage());
        if (item.getType() == ShopItemType.RANDOM_SEED && user.getUnlockedPlants().isEmpty())
            throw new Exception(SystemMessage.SHOP_NO_UNLOCKED_PLANTS.getMessage());
        if (item.getType() == ShopItemType.SELECT_SEED) {
            PlayerPlant plant = user.getOwnedPlant(plantType);
            if (plant == null) throw new Exception(SystemMessage.SHOP_PLANT_NOT_UNLOCKED.getMessage());
            if ((long) plant.getSeedPackets() + 10 > Integer.MAX_VALUE)
                throw new Exception("Seed capacity reached.");
        }
        if (item.getType() == ShopItemType.DIAMOND_TO_COIN
                && (long) user.getCoins() + 500 > Integer.MAX_VALUE)
            throw new Exception("Coin capacity reached.");
        if (user.getCoins() < item.getCoinPrice())
            throw new Exception(SystemMessage.SHOP_NOT_ENOUGH_COINS.getMessage());
        if (user.getDiamonds() < item.getDiamondPrice())
            throw new Exception(SystemMessage.SHOP_NOT_ENOUGH_DIAMONDS.getMessage());
    }

    private void buyPot(User user, int count, int totalCoin) throws Exception {
        int lockedPots =
                user.getGreenhouse().getLockedPotCount();

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

        long addedSeeds = (long) count * 10L;
        long finalSeeds = (long) targetPlant.getSeedPackets() + addedSeeds;

        if (finalSeeds > Integer.MAX_VALUE) {
            throw new Exception("Invalid quantity resulting in overflow.");
        }

        if (!user.spendDiamonds(totalDiamond)) {
            throw new Exception(SystemMessage.SHOP_NOT_ENOUGH_DIAMONDS.getMessage());
        }
        targetPlant.addSeedPackets((int) addedSeeds);
    }

    private void buyDiamondExchange(User user, int count, int totalDiamond) throws Exception {
        long addedCoins = (long) count * 500L;
        long finalCoins = (long) user.getCoins() + addedCoins;

        if (finalCoins > Integer.MAX_VALUE) {
            throw new Exception("Invalid quantity resulting in overflow.");
        }

        if (!user.spendDiamonds(totalDiamond)) {
            throw new Exception(SystemMessage.SHOP_NOT_ENOUGH_DIAMONDS.getMessage());
        }
        user.addCoins((int) addedCoins);
    }

    private void buyDailyOffer(User user, int count, DailyOffer expectedOffer) throws Exception {
        if (count > 1) {
            throw new Exception(SystemMessage.SHOP_DAILY_OFFER_ONCE.getMessage());
        }

        if (expectedOffer != null && (user.getDailyOffer() != expectedOffer
                || !expectedOffer.getDate().equals(LocalDate.now()))) {
            throw new Exception("Daily offer changed. Please review the new offer.");
        }
        DailyOffer offer = expectedOffer != null ? expectedOffer
                : getOrGenerateDailyOffer(user).offer();
        if (offer.isPurchased()) {
            throw new Exception(SystemMessage.SHOP_DAILY_OFFER_ALREADY_BOUGHT.getMessage());
        }

        PlayerPlant targetPlant = user.getOwnedPlant(offer.getPlantName());
        if (targetPlant == null) {
            throw new Exception(SystemMessage.SHOP_PLANT_NOT_UNLOCKED.getMessage());
        }

        if (!user.spendCoins(offer.getPrice())) {
            throw new Exception(SystemMessage.SHOP_NOT_ENOUGH_COINS.getMessage());
        }

        targetPlant.addSeedPackets(10);
        offer.setPurchased(true);
    }

    public DailyOfferResult getOrGenerateDailyOffer(User user) throws Exception {
        DailyOffer currentOffer = user.getDailyOffer();
        LocalDate today = LocalDate.now();

        if (currentOffer == null || !currentOffer.getDate().equals(today)) {
            List<PlayerPlant> unlocked = user.getUnlockedPlants();
            if (unlocked.isEmpty()) {
                throw new Exception(SystemMessage.SHOP_DAILY_OFFER_NO_PLANTS.getMessage());
            }

            PlayerPlant randomPlant = unlocked.get(random.nextInt(unlocked.size()));
            currentOffer =
                    new DailyOffer(
                            randomPlant.getPlantName(),
                            DAILY_OFFER_PRICE,
                            today);

            user.setDailyOffer(currentOffer);

            return new DailyOfferResult(currentOffer, true);
        }

        return new DailyOfferResult(currentOffer, false);
    }
}
