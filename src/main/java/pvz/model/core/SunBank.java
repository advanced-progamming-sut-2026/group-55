package pvz.model.core;

public final class SunBank {
    private int balance;

    public SunBank(int initialBalance) {
        this.balance = initialBalance;
    }

    public int getBalance() {
        return balance;
    }

    public boolean canAfford(int amount) {
        return balance >= amount;
    }

    public void add(int amount) {
        balance += amount;
    }

    public boolean spend(int amount) {
        if (!canAfford(amount)) {
            return false;
        }
        balance -= amount;
        return true;
    }
}
