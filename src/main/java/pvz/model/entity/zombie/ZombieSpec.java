package pvz.model.entity.zombie;

import java.util.List;
import java.util.Objects;

public final class ZombieSpec {
    private final String id;
    private final String name;
    private final int hitpoints;
    private final int eatDps;
    private final double speed;
    private final int waveCost;
    private final int waveWeight;
    private final List<String> armorIds;
    private final List<String> behaviorTypes;
    private final boolean implemented;

    public ZombieSpec(
            String id,
            String name,
            int hitpoints,
            int eatDps,
            double speed,
            int waveCost,
            int waveWeight,
            List<String> armorIds,
            List<String> behaviorTypes,
            boolean implemented
    ) {
        this.id = requireText(id, "zombie id");
        this.name = requireText(name, "zombie name");
        if (hitpoints <= 0 || eatDps < 0 || speed < 0
                || waveCost <= 0 || waveWeight <= 0) {
            throw new IllegalArgumentException(
                    "invalid numeric values for zombie " + id
            );
        }
        this.hitpoints = hitpoints;
        this.eatDps = eatDps;
        this.speed = speed;
        this.waveCost = waveCost;
        this.waveWeight = waveWeight;
        this.armorIds = List.copyOf(armorIds);
        this.behaviorTypes = List.copyOf(behaviorTypes);
        this.implemented = implemented;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " cannot be null");
        String result = value.strip();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return result;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getHitpoints() { return hitpoints; }
    public int getEatDps() { return eatDps; }
    public double getSpeed() { return speed; }
    public int getWaveCost() { return waveCost; }
    public int getWaveWeight() { return waveWeight; }
    public List<String> getArmorIds() { return armorIds; }
    public List<String> getBehaviorTypes() { return behaviorTypes; }
    public boolean isImplemented() { return implemented; }

    public String getArmor() {
        return armorIds.isEmpty() ? "NONE" : String.join("|", armorIds);
    }
}
