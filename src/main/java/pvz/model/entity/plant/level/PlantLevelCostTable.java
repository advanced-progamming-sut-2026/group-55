package pvz.model.entity.plant.level;

import java.util.Map;
import java.util.Objects;

public final class PlantLevelCostTable {
    private static final Map<Integer, PlantLevelCost> DEFAULT_COSTS = Map.of(
            2, new PlantLevelCost(2, 1_000, 10),
            3, new PlantLevelCost(3, 5_000, 75),
            4, new PlantLevelCost(4, 10_000, 200)
    );

    private final Map<Integer, PlantLevelCost> costs;

    public PlantLevelCostTable(Map<Integer, PlantLevelCost> costs) {
        Objects.requireNonNull(costs, "costs cannot be null");
        for (int level = 2; level <= 4; level++) {
            if (!costs.containsKey(level)) {
                throw new IllegalArgumentException("missing upgrade cost for target level " + level);
            }
        }
        this.costs = Map.copyOf(costs);
    }

    public static PlantLevelCostTable defaults() {
        return new PlantLevelCostTable(DEFAULT_COSTS);
    }

    public PlantLevelCost forTargetLevel(int targetLevel) {
        PlantLevelCost cost = costs.get(targetLevel);
        if (cost == null) {
            throw new IllegalArgumentException("no upgrade cost for target level " + targetLevel);
        }
        return cost;
    }

    public Map<Integer, PlantLevelCost> asMap() {
        return costs;
    }
}
