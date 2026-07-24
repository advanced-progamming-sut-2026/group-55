package pvz.model.account;

public class PlayerPlant {

    private final String plantName;
    private int level;
    private int seedPackets = 0;

    private boolean boosted;

    public PlayerPlant(String plantName) {
        this.plantName = plantName;
        this.level = 1;
        this.seedPackets = 0;
        this.boosted = false;
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
        this.seedPackets += amount;
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


    public boolean isBoosted() {
        return boosted;
    }

    public void giveBoost() {
        this.boosted = true;
    }

    public void consumeBoost() {
        this.boosted = false;
    }
}