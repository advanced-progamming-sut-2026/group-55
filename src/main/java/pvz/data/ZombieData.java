package pvz.data;

import java.util.List;
import java.util.Map;
import pvz.model.entity.zombie.ArmorSpec;
import pvz.model.entity.zombie.ZombieBehaviorDefinition;
import pvz.model.entity.zombie.ZombieSpec;

public record ZombieData(
        Map<String, ZombieSpec> byName,
        Map<String, ZombieSpec> byId,
        Map<String, ArmorSpec> armorSpecs,
        Map<String, List<ZombieBehaviorDefinition>> behaviorsByZombieId
) {
    public ZombieData {
        byName = Map.copyOf(byName);
        byId = Map.copyOf(byId);
        armorSpecs = Map.copyOf(armorSpecs);
        behaviorsByZombieId = Map.copyOf(behaviorsByZombieId);
    }
}
