package pvz.model.greenhouse;

public class GreenhousePlant {
    private String plantName;
    private boolean isMarigold;
    private long plantedAt;
    private long readyAt;

    public GreenhousePlant(String plantName, boolean isMarigold, long growTimeMillis) {
        this.plantName = plantName;
        this.isMarigold = isMarigold;
        this.plantedAt = System.currentTimeMillis();
        this.readyAt = this.plantedAt + growTimeMillis;
    }

    public String getPlantName() { return plantName; }
    public boolean isMarigold() { return isMarigold; }
    public long getPlantedAt() { return plantedAt; }
    public long getReadyAt() { return readyAt; }

    public boolean isReady() {
        return System.currentTimeMillis() >= readyAt;
    }

    public int getRemainingHours() {
        long remainingMillis = readyAt - System.currentTimeMillis();
        if (remainingMillis <= 0) return 0;
        return (int) Math.ceil((double) remainingMillis / (1000 * 60 * 60));
    }

    public void forceReady() {
        this.readyAt = System.currentTimeMillis();
    }
}