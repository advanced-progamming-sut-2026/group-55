package pvz.model.session;

import java.util.Objects;
import pvz.model.account.User;
import pvz.model.core.BattleResources;
import pvz.model.core.BattleWallet;

public final class BattleRewardSettlement {
    public Result settle(BattleResources resources, User user) {
        Objects.requireNonNull(resources, "battle resources cannot be null");
        Objects.requireNonNull(user, "user cannot be null");

        BattleWallet wallet = resources.battleWallet();
        validateCurrencyCapacity(wallet, user);

        int returnedPlantFood = resources.getPlantFoodCount();
        if (!user.addPlantFood(returnedPlantFood)) {
            throw new IllegalStateException(
                    "remaining plant food exceeds persistent capacity"
            );
        }

        int unlockedPots = user.getGreenhouse().unlockPots(
                resources.getCollectedPotCount()
        );
        wallet.transferTo(user);
        user.incrementGamesPlayed();

        return new Result(
                wallet.getCollectedCoins(),
                wallet.getCollectedDiamonds(),
                resources.getCollectedPotCount(),
                unlockedPots,
                returnedPlantFood
        );
    }

    private void validateCurrencyCapacity(
            BattleWallet wallet,
            User user
    ) {
        Math.addExact(
                user.getCoins(),
                wallet.getCollectedCoins()
        );
        Math.addExact(
                user.getDiamonds(),
                wallet.getCollectedDiamonds()
        );
    }

    public record Result(
            int coins,
            int diamonds,
            int collectedPots,
            int unlockedPots,
            int returnedPlantFood
    ) {
    }
}
