package pvz.model.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        BattleRewardSettlement.Result result =
                new BattleRewardSettlement().settle(resources, user);

        assertEquals(50, user.getCoins());
        assertEquals(1, user.getDiamonds());
        assertEquals(2, user.getPlantFoodCount());
        assertEquals(1, user.getGamesPlayed());
        assertEquals(1, result.unlockedPots());
        assertEquals(6, user.getGreenhouse().getUnlockedPotCount());
    }
}
