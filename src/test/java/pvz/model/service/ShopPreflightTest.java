package pvz.model.service;

import org.junit.jupiter.api.Test;
import pvz.model.account.User;
import pvz.model.shop.DailyOffer;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ShopPreflightTest {
    private User user() {
        User user = new User("test", "hash", "Tester", "test@example.com", "other");
        user.addCoins(10000);
        user.addDiamonds(100);
        return user;
    }

    @Test void staleConfirmationNeverChargesOrReplacesOffer() {
        User user = user();
        ShopService service = new ShopService();
        DailyOffer old = new DailyOffer(user.getUnlockedPlants().get(0).getPlantName(),
                1600, LocalDate.now().minusDays(1));
        user.setDailyOffer(old);
        int coins = user.getCoins();
        assertThrows(Exception.class, () -> service.buy(user, 6, 1, null, old));
        assertEquals(coins, user.getCoins());
        assertSame(old, user.getDailyOffer());
        assertFalse(old.isPurchased());
    }

    @Test void preflightDoesNotMutateAndDailyPurchaseOnlyWorksOnce() throws Exception {
        User user = user();
        ShopService service = new ShopService();
        DailyOffer offer = service.getOrGenerateDailyOffer(user).offer();
        int coins = user.getCoins();
        int seeds = user.getOwnedPlant(offer.getPlantName()).getSeedPackets();
        service.validateSinglePurchase(user, 6, null, offer);
        assertEquals(coins, user.getCoins());
        assertFalse(offer.isPurchased());
        service.buy(user, 6, 1, null, offer);
        assertEquals(coins - 1600, user.getCoins());
        assertEquals(seeds + 10, user.getOwnedPlant(offer.getPlantName()).getSeedPackets());
        assertThrows(Exception.class, () -> service.buy(user, 6, 1, null, offer));
        assertEquals(coins - 1600, user.getCoins());
    }

    @Test void capacityAndFundsAreCheckedWithoutSpending() {
        User user = user();
        ShopService service = new ShopService();
        user.getGreenhouse().unlockPots(8);
        int coins = user.getCoins();
        assertThrows(Exception.class, () -> service.validateSinglePurchase(user, 1, null, null));
        assertEquals(coins, user.getCoins());
        user.spendDiamonds(user.getDiamonds());
        assertThrows(Exception.class, () -> service.validateSinglePurchase(user, 4,
                user.getUnlockedPlants().get(0).getPlantName(), null));
        assertEquals(0, user.getDiamonds());
    }
}
