package pvz.model.entity.zombie;

import java.util.ArrayList;
import java.util.List;

public final class ArmorSet {
    private final List<ArmorInstance> layers;

    public ArmorSet(List<ArmorSpec> specs) {
        layers = new ArrayList<>(
                specs.stream().map(ArmorInstance::new).toList()
        );
    }

    public ArmorDamageResult absorb(double damage) {
        double remaining = Math.max(0, damage);
        List<ArmorInstance> broken = new ArrayList<>();

        for (ArmorInstance layer : layers) {
            if (remaining <= 0) {
                break;
            }
            boolean wasIntact = !layer.isBroken();
            remaining = layer.absorb(remaining);
            if (wasIntact && layer.isBroken()) {
                broken.add(layer);
            }
        }
        return new ArmorDamageResult(remaining, List.copyOf(broken));
    }

    public List<ArmorInstance> layers() {
        return List.copyOf(layers);
    }

    public boolean hasIntactArmor() {
        return layers.stream().anyMatch(layer -> !layer.isBroken());
    }

    public boolean hasArmor(String id) {
        return layers.stream().anyMatch(
                layer -> layer.spec().id().equalsIgnoreCase(id)
        );
    }

    public boolean hasIntactArmor(String id) {
        return layers.stream().anyMatch(
                layer -> !layer.isBroken()
                        && layer.spec().id().equalsIgnoreCase(id)
        );
    }

    public void add(ArmorSpec spec) {
        layers.add(new ArmorInstance(spec));
    }

    public record ArmorDamageResult(
            double overflowDamage,
            List<ArmorInstance> brokenLayers
    ) {}
}
