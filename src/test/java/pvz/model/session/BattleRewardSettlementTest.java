package pvz.model.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import pvz.model.account.User;
import pvz.model.core.BattleResources;

class BattleRewardSettlementTest {
    @Test
    void transfersEveryStageRewardExactlyOnce() {
        User user = new User(
                "player",
                "hash",
                "Player",
                "p@example.com",
                "x"
        );
        BattleResources resources = new BattleResources(50, 2);
        resources.battleWallet().addCoins(50);
        resources.battleWallet().addDiamonds(1);
        resources.addCollectedPot();

        BattleRewardSettlement settlement = new BattleRewardSettlement();
        BattleRewardSettlement.Result result = settlement.settle(
                resources,
                user
        );

        assertEquals(50, user.getCoins());
        assertEquals(1, user.getDiamonds());
        assertEquals(2, user.getPlantFoodCount());
        assertEquals(1, user.getGamesPlayed());
        assertEquals(1, result.unlockedPots());
        assertEquals(5, user.getGreenhouse().getUnlockedPotCount());

        assertThrows(
                IllegalStateException.class,
                () -> settlement.settle(resources, user)
        );
        assertEquals(50, user.getCoins());
        assertEquals(1, user.getDiamonds());
        assertEquals(2, user.getPlantFoodCount());
        assertEquals(1, user.getGamesPlayed());
        assertEquals(5, user.getGreenhouse().getUnlockedPotCount());
    }
}
