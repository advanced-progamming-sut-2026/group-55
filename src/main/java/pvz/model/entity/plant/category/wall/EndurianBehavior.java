package pvz.model.entity.plant.category.wall;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.capability.PlantHitReactionCapability;
import pvz.model.entity.plant.hit.PlantHitContext;

final class EndurianBehavior extends ArmoredWallBehavior
        implements PlantHitReactionCapability {

    private static final double ARMORED_REFLECT_MULTIPLIER = 2;

    private final double baseReflectDamage;

    EndurianBehavior(
            Plant owner,
            boolean blocksVaulting,
            double armorCapacity,
            double baseReflectDamage
    ) {
        super(owner, blocksVaulting, armorCapacity);

        if (baseReflectDamage <= 0) {
            throw new IllegalArgumentException(
                    "reflect damage must be positive"
            );
        }

        this.baseReflectDamage = baseReflectDamage;
    }

    @Override
    public void onPlantHit(PlantHitContext context) {
        if (!context.isBite()) {
            return;
        }

        context.attacker().takeDirectDamage(currentReflectDamage());
    }

    double currentReflectDamage() {
        return hasArmor()
                ? baseReflectDamage * ARMORED_REFLECT_MULTIPLIER
                : baseReflectDamage;
    }
}
