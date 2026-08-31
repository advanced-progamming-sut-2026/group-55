package pvz.model.greenhouse;

public class GreenhousePlant {
    private static final long HOUR_IN_MILLIS = 3600000;
    private final String plantName;
    private final boolean isMarigold;
    private long readyAt;

    public GreenhousePlant(String plantName, boolean isMarigold, long growTimeMillis) {
        this.plantName = plantName;
        this.isMarigold = isMarigold;
        this.readyAt = System.currentTimeMillis() + growTimeMillis;
    }

    public String getPlantName() {
        return plantName;
    }

    public boolean isMarigold() {
        return isMarigold;
    }

    public boolean isReady() {
        return System.currentTimeMillis() >= readyAt;
    }

    public int getRemainingHours() {
        long remaining = readyAt - System.currentTimeMillis();
        if (remaining <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) remaining / HOUR_IN_MILLIS);
    }

    public String getExactRemainingTime() {
        long remaining = readyAt - System.currentTimeMillis();
        if (remaining <= 0) {
            return "Ready";
        }

        long totalSeconds = remaining / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        StringBuilder timeString = new StringBuilder();
        if (hours > 0) {
            timeString.append(hours).append("h ");
        }
            timeString.append(minutes).append("m ");
        return timeString.toString().trim();
    }

    public void forceReady() {
        this.readyAt = System.currentTimeMillis();
    }
}
