package pvz.data;

import java.util.Map;
import pvz.model.entity.zombie.ZombieSpec;

public record ZombieData(
        Map<String, ZombieSpec> byName,
        Map<String, ZombieSpec> byId
) {}
