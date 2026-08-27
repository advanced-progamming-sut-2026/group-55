package pvz.model.entity.plant;

import java.util.Objects;

import pvz.model.entity.plant.behavior.PlantBehavior;
import pvz.model.entity.plant.hit.PlantHitContext;
import pvz.model.entity.plant.hit.PlantHitSource;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.zombie.Zombie;

/**
 * Shared damage path of a plant. Every accepted hit passes the armor of the
 * behavior first, then the plant health, and finally notifies the behavior
 * reaction exactly once. Hits blocked by the lifecycle never reach here.
 */
final class PlantHitPipeline {

    private final Plant owner;

    private final PlantBehavior behavior;

    private boolean hitInProgress;

    private double armorAbsorbedInHit;

    PlantHitPipeline(Plant owner, PlantBehavior behavior) {
        this.owner = Objects.requireNonNull(owner, "owner cannot be null");
        this.behavior = Objects.requireNonNull(
                behavior,
                "behavior cannot be null"
        );
    }

    boolean receiveHit(
            PlantHitSource source,
            Zombie attacker,
            double damage,
            long currentTick
    ) {
        Objects.requireNonNull(source, "plant hit source cannot be null");

        if (!(damage > 0)
                || owner.isRemovedFromWorld()
                || !owner.canBeAffectedBy(PlantThreat.DAMAGE)) {
            return false;
        }

        double healthBeforeHit = owner.getHealth();

        armorAbsorbedInHit = 0;
        hitInProgress = true;

        try {
            owner.takeDamage(damage);
        } finally {
            hitInProgress = false;
        }

        PlantCapabilities.notifyHit(behavior, new PlantHitContext(
                source,
                attacker,
                damage,
                armorAbsorbedInHit,
                healthBeforeHit - owner.getHealth(),
                currentTick,
                true
        ));

        return true;
    }

    double modifyIncomingDamage(double damage) {
        double remainingDamage = PlantCapabilities.modifyIncomingDamage(
                behavior,
                damage
        );

        if (hitInProgress) {
            armorAbsorbedInHit += Math.max(0, damage - remainingDamage);
        }

        return remainingDamage;
    }
}
