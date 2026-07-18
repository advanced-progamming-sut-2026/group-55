package pvz.model.entity.plant;

import java.util.Locale;
import java.util.Map;

public class PlantFactory {
    private final Map<String, PlantSpec> specs;
    public PlantFactory(Map<String, PlantSpec> specs) {
        this.specs = specs;
    }

    public Plant create(String type) {
        PlantSpec spec = specs.get(type.toLowerCase(Locale.ROOT));
        return (spec == null) ? null : new Plant(spec);
    }
}
