package pvz.model.greenhouse;

public class Greenhouse {
    private Pot[][] pots;

    public Greenhouse() {
        pots = new Pot[4][5];
        for (int y = 1; y <= 4; y++) {
            for (int x = 1; x <= 5; x++) {

                boolean isLocked = (y != 1);

                pots[y - 1][x - 1] = new Pot(x, y, isLocked);
            }
        }
    }

    public Pot getPot(int x, int y) {
        if (x < 1 || x > 5 || y < 1 || y > 4) {
            return null;
        }
        return pots[y - 1][x - 1];
    }

    public void unlockRow(int y) {
        if (y < 1 || y > 4) return;
        for (int x = 1; x <= 5; x++) {
            pots[y - 1][x - 1].unlock();
        }
    }

    public void updateAllPots() {
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 5; x++) {
                pots[y][x].updateState();
            }
        }
    }

    public Pot[][] getPots() {
        return pots;
    }


    public boolean unlockNextAvailablePot() {
        for (int y = 2; y <= 4; y++) {
            for (int x = 1; x <= 5; x++) {
                Pot pot = getPot(x, y);
                if (pot != null && pot.isLocked()) {
                    pot.unlock();
                    return true;
                }
            }
        }
        return false;
    }
}