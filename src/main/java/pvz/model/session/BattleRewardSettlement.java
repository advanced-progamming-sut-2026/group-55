package pvz.model.session;

import java.util.Objects;
import pvz.model.account.User;
import pvz.model.core.BattleResources;
import pvz.model.core.BattleWallet;

public final class BattleRewardSettlement {
    public Result settle(BattleResources resources, User user) {
        return settle(resources, user, true);
    }

    public Result settle(
            BattleResources resources,
            User user,
            boolean returnPlantFood
    ) {
        validate(resources, user, returnPlantFood);

        BattleWallet wallet = resources.battleWallet();
        int returnedPlantFood = 0;
        if (returnPlantFood) {
            returnedPlantFood = returnRemainingPlantFood(resources, user);
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

    public int returnRemainingPlantFood(
            BattleResources resources,
            User user
    ) {
        Objects.requireNonNull(resources, "battle resources cannot be null");
        Objects.requireNonNull(user, "user cannot be null");

        if (resources.isPlantFoodReturned()) {
            return 0;
        }

        int amount = resources.getPlantFoodCount();
        if (!user.canAddPlantFood(amount)) {
            throw new IllegalStateException(
                    "remaining plant food exceeds persistent capacity"
            );
        }
        if (!user.addPlantFood(amount)) {
            throw new IllegalStateException(
                    "remaining plant food could not be returned"
            );
        }
        resources.markPlantFoodReturned();
        return amount;
    }

    void validate(
            BattleResources resources,
            User user,
            boolean returnPlantFood
    ) {
        Objects.requireNonNull(resources, "battle resources cannot be null");
        Objects.requireNonNull(user, "user cannot be null");

        BattleWallet wallet = resources.battleWallet();
        if (wallet.isTransferred()) {
            throw new IllegalStateException(
                    "battle rewards have already been settled"
            );
        }

        validateCurrencyCapacity(wallet, user);
        if (returnPlantFood
                && !resources.isPlantFoodReturned()
                && !user.canAddPlantFood(resources.getPlantFoodCount())) {
            throw new IllegalStateException(
                    "remaining plant food exceeds persistent capacity"
            );
        }
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
