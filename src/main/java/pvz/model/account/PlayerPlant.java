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
        long newAmount = (long) this.seedPackets + amount;
        if (newAmount > Integer.MAX_VALUE) {
            this.seedPackets = Integer.MAX_VALUE; // قفل شدن روی حداکثر مقدار ممکن
        } else {
            this.seedPackets = (int) newAmount;
        }
    }

    public boolean spendSeedPackets(int amount) {
        if (this.seedPackets >= amount) {
            this.seedPackets -= amount;
            return true;
        }
        return false;
    }

    public void upgrade() {
        this.level++;
    }
}
