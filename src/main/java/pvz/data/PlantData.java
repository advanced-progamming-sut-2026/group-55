package pvz.data;

import java.util.Map;
import pvz.model.entity.plant.PlantSpec;

public record PlantData(
        Map<String, PlantSpec> byName,
        Map<Integer, PlantSpec> byId
) {}