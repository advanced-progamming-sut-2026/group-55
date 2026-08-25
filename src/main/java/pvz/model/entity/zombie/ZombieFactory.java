package pvz.model.entity.zombie;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import pvz.data.ZombieData;
import pvz.model.entity.zombie.behavior.ZombieBehavior;
import pvz.model.entity.zombie.behavior.ZombieBehaviorFactory;

public final class ZombieFactory {
    private final Map<String, ZombieSpec> specsByName;
    private final Map<String, ZombieSpec> specsById;
    private final Map<String, ArmorSpec> armorSpecs;
    private final Map<String, List<ZombieBehaviorDefinition>> behaviorDefinitions;
    private final ZombieBehaviorFactory behaviorFactory;

    public ZombieFactory(ZombieData data) {
        this(data, new ZombieBehaviorFactory());
    }

    ZombieFactory(ZombieData data, ZombieBehaviorFactory behaviorFactory) {
        Objects.requireNonNull(data, "zombie data cannot be null");
        this.specsByName = data.byName();
        this.specsById = data.byId();
        this.armorSpecs = data.armorSpecs();
        this.behaviorDefinitions = data.behaviorsByZombieId();
        this.behaviorFactory = Objects.requireNonNull(
                behaviorFactory,
                "behavior factory cannot be null"
        );
        validateSupportedBehaviorDefinitions();
    }

    public Zombie create(String type) {
        return create(type, 3);
    }

    public Zombie create(String type, int difficultyLevel) {
        Objects.requireNonNull(type, "zombie type cannot be null");
        String key = type.strip().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            throw new IllegalArgumentException("zombie type cannot be blank");
        }

        ZombieSpec spec = specsById.get(key);
        if (spec == null) {
            spec = specsByName.get(key);
        }
        if (spec == null) {
            return null;
        }
        if (!spec.isImplemented()) {
            throw new UnsupportedOperationException(
                    spec.getName() + " zombie behavior is not implemented yet"
            );
        }

        ZombieRuntimeStats runtimeStats = ZombieRuntimeStats.from(
                spec,
                difficultyLevel
        );
        double healthMultiplier = difficultyLevel / 3.0;
        List<ArmorSpec> armors = spec.getArmorIds().stream()
                .map(armorSpecs::get)
                .map(armor -> new ArmorSpec(
                        armor.id(),
                        armor.name(),
                        armor.maxHealth() * healthMultiplier,
                        armor.metallic()
                ))
                .toList();
        List<ZombieBehavior> behaviors = behaviorDefinitions
                .getOrDefault(keyOf(spec.getId()), List.of())
                .stream()
                .map(definition -> behaviorFactory.create(
                        definition,
                        healthMultiplier
                ))
                .toList();

        return new Zombie(
                spec,
                runtimeStats,
                new ArmorSet(armors),
                behaviors
        );
    }

    public ZombieSpec getSpecById(String id) {
        Objects.requireNonNull(id, "zombie id cannot be null");
        return specsById.get(keyOf(id.strip()));
    }

    private String keyOf(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private void validateSupportedBehaviorDefinitions() {
        for (ZombieSpec spec : specsById.values()) {
            if (!spec.isImplemented()) {
                continue;
            }
            for (ZombieBehaviorDefinition definition : behaviorDefinitions
                    .getOrDefault(keyOf(spec.getId()), List.of())) {
                behaviorFactory.create(definition);
            }
        }
    }
}
