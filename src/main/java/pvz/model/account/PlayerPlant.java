package pvz.model.account;

public class PlayerPlant {
    private String plantName;
    private int level;
    private int seedPackets;

    public PlayerPlant(String plantName) {
        this.plantName = plantName;
        this.level = 1;
        this.seedPackets = 0;
    }

    public String getPlantName() {
        return plantName;
    }

    public int getLevel() {
        return level;
    }

    public int getSeedPackets() {
        return seedPackets;
    }

    public void addSeedPackets(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("seed packet amount cannot be negative");
        }
        long newAmount = (long) this.seedPackets + amount;
        if (newAmount > Integer.MAX_VALUE) {
            this.seedPackets = Integer.MAX_VALUE;
        } else {
            this.seedPackets = (int) newAmount;
        }
    }

    public boolean spendSeedPackets(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("seed packet cost cannot be negative");
        }
        if (this.seedPackets >= amount) {
            this.seedPackets -= amount;
            return true;
        }
        return false;
    }

    public void upgrade() {
        if (this.level >= 4) {
            throw new IllegalStateException("plant is already at maximum level");
        }
        this.level++;
    }
}
