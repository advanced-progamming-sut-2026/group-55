package pvz.model.entity.zombie.behavior;

import pvz.model.entity.zombie.ZombieBehaviorDefinition;

public final class ZombieBehaviorFactory {
    public ZombieBehavior create(ZombieBehaviorDefinition definition) {
        return switch (definition.type()) {
            case "TOMB_SPAWN" -> new TombSpawnBehavior(
                    definition.requirePositiveInt("intervalSeconds"),
                    definition.requirePositiveInt("tombsPerCast")
            );
            default -> throw new IllegalArgumentException(
                    "unknown or unimplemented zombie behavior: "
                            + definition.type()
            );
        };
    }
}
