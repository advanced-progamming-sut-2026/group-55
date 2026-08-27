package pvz.model.entity.plant.hit;

import java.util.Objects;

import pvz.model.entity.zombie.Zombie;

public record PlantHitContext(
        PlantHitSource source,
        Zombie attacker,
        double incomingDamage,
        double armorAbsorbedDamage,
        double healthDamage,
        long tick,
        boolean accepted
) {
    public PlantHitContext {
        Objects.requireNonNull(source, "plant hit source cannot be null");

        if (incomingDamage < 0
                || armorAbsorbedDamage < 0
                || healthDamage < 0) {
            throw new IllegalArgumentException(
                    "plant hit damage values cannot be negative"
            );
        }

        if (tick < 0) {
            throw new IllegalArgumentException(
                    "plant hit tick cannot be negative"
            );
        }
    }

    public boolean isBite() {
        return source == PlantHitSource.BITE && attacker != null;
    }
}
