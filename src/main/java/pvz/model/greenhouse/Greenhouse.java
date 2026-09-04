package pvz.model.greenhouse;

public class Greenhouse {

    private static final int ROWS = 3;
    private static final int COLS = 4;

    private final Pot[][] pots = new Pot[ROWS][COLS];

    public Greenhouse() {
        for (int y = 1; y <= ROWS; y++) {
            for (int x = 1; x <= COLS; x++) {
                pots[y - 1][x - 1] = new Pot(x, y, y != 1);
            }
        }
    }

    public Pot getPot(int x, int y) {
        if (x < 1 || x > COLS || y < 1 || y > ROWS) {
            return null;
        }

        return pots[y - 1][x - 1];
    }

    public void updateAllPots() {
        for (Pot[] row : pots) {
            for (Pot pot : row) {
                pot.updateState();
            }
        }
    }

    public boolean unlockNextAvailablePot() {
        for (int y = 2; y <= ROWS; y++) {
            for (int x = 1; x <= COLS; x++) {
                Pot pot = getPot(x, y);

                if (pot != null && pot.isLocked()) {
                    pot.unlock();
                    return true;
                }
            }
        }

        return false;
    }

    public int unlockPots(int count) {
        if (count < 0) {
            throw new IllegalArgumentException(
                    "pot count cannot be negative"
            );
        }

        int unlocked = 0;

        while (unlocked < count && unlockNextAvailablePot()) {
            unlocked++;
        }

        return unlocked;
    }

    public int getUnlockedPotCount() {
        int unlocked = 0;

        for (Pot[] row : pots) {
            for (Pot pot : row) {
                if (!pot.isLocked()) {
                    unlocked++;
                }
            }
        }

        return unlocked;
    }
}
