package pvz.model.currency;

public interface CurrencyWallet {
    int getCoins();

    int getDiamonds();

    void addCoins(int amount);

    void addDiamonds(int amount);
}
