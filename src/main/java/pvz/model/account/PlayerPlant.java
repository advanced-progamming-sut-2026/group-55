package pvz.model.account;

public class PlayerPlant {
    private String plantName;
    private int level;

    public PlayerPlant(String plantName) {
        this.plantName = plantName;
        this.level = 1;
    }

    public String getPlantName() {
        return plantName;
    }

    public int getLevel() {
        return level;
    }

    public void upgrade() {
        this.level++;
    }
}