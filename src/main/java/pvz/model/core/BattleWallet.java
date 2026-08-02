package pvz.model.core;

import java.util.Objects;
import pvz.model.currency.CurrencyWallet;

public final class BattleWallet {
    private int collectedCoins;
    private int collectedDiamonds;
    private boolean transferred;

    public void addCoins(int amount) {
        requireNonNegative(amount, "coin amount");
        ensureNotTransferred();
        collectedCoins = Math.addExact(collectedCoins, amount);
    }

    public void addDiamonds(int amount) {
        requireNonNegative(amount, "diamond amount");
        ensureNotTransferred();
        collectedDiamonds = Math.addExact(collectedDiamonds, amount);
    }

    public int getCollectedCoins() {
        return collectedCoins;
    }

    public int getCollectedDiamonds() {
        return collectedDiamonds;
    }

    public boolean hasCollectedCurrency() {
        return collectedCoins > 0 || collectedDiamonds > 0;
    }

    public void transferTo(CurrencyWallet wallet) {
        Objects.requireNonNull(wallet, "currency wallet cannot be null");
        ensureNotTransferred();

        Math.addExact(wallet.getCoins(), collectedCoins);
        Math.addExact(wallet.getDiamonds(), collectedDiamonds);

        wallet.addCoins(collectedCoins);
        wallet.addDiamonds(collectedDiamonds);
        transferred = true;
    }

    public boolean isTransferred() {
        return transferred;
    }

    private void ensureNotTransferred() {
        if (transferred) {
            throw new IllegalStateException(
                    "battle currency has already been transferred"
            );
        }
    }

    private void requireNonNegative(int amount, String fieldName) {
        if (amount < 0) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be negative"
            );
        }
    }
}
