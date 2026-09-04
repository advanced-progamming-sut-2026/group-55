package pvz.model.entity.plant.level;

public record PlantLevelCost(int targetLevel, int coins, int seedPackets) {
    public PlantLevelCost {
        if (targetLevel < 2 || targetLevel > 4) {
            throw new IllegalArgumentException("targetLevel must be between 2 and 4");
        }
        if (coins < 0 || seedPackets < 0) {
            throw new IllegalArgumentException("upgrade costs cannot be negative");
        }
    }
}
