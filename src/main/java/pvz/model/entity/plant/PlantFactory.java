package pvz.model.entity.plant;

import java.util.Locale;
import java.util.Map;

public class PlantFactory {
    private final Map<String, PlantSpec> specs;

    public PlantFactory(Map<String, PlantSpec> specs) {
        this.specs = specs;
    }

    public Plant create(String type) {
        return create(type, PlantSpec.MIN_LEVEL);
    }

    public Plant create(String type, int level) {
        PlantSpec spec = getSpec(type);
        return (spec == null) ? null : new Plant(spec.withLevel(level));
    }

    public PlantSpec getSpec(String type) {
        if (type == null) {
            return null;
        }
        return specs.get(type.toLowerCase(Locale.ROOT));
    }
}
