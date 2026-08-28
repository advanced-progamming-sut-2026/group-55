package pvz.data;

import java.util.Map;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.level.PlantLevelCostTable;

public record PlantData(
        Map<String, PlantSpec> byName,
        Map<Integer, PlantSpec> byId,
        PlantLevelCostTable levelCosts
) {}
