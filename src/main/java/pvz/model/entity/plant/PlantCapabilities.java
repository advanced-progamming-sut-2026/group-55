package pvz.model.entity.plant;

import pvz.model.entity.plant.behavior.PlantBehavior;
import pvz.model.entity.plant.behavior.capability.DamageModifierCapability;
import pvz.model.entity.plant.behavior.capability.PlantArmorCapability;
import pvz.model.entity.plant.behavior.capability.PlantActivationCapability;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.plant.behavior.capability.PlantHitReactionCapability;
import pvz.model.entity.plant.behavior.capability.ContactTriggerCapability;
import pvz.model.entity.plant.behavior.capability.SunProductionCapability;
import pvz.model.entity.plant.behavior.capability.TargetTilePlacementCapability;
import pvz.model.entity.plant.behavior.capability.TransientEffectCapability;
import pvz.model.entity.plant.behavior.capability.ZombieEdibilityCapability;
import pvz.model.entity.plant.behavior.capability.VaultBlockingCapability;
import pvz.model.entity.plant.hit.PlantHitContext;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.plant.placement.PlantPlacementTarget;

final class PlantCapabilities {

    private PlantCapabilities() {
    }

    static PlantFoodCapability plantFood(PlantBehavior behavior) {
        if (!(behavior instanceof PlantFoodCapability capability)
                || !capability.supportsPlantFood()) {
            return null;
        }

        return capability;
    }


    static boolean isActivationActive(PlantBehavior behavior) {
        return behavior instanceof PlantActivationCapability capability
                && capability.isActivationActive();
    }

    static boolean blocksPlantFoodDuringActivation(
            PlantBehavior behavior
    ) {
        return behavior instanceof PlantActivationCapability capability
                && capability.isActivationActive()
                && capability.blocksPlantFoodDuringActivation();
    }

    static boolean blocksThreatDuringActivation(
            PlantBehavior behavior,
            PlantThreat threat
    ) {
        return behavior instanceof PlantActivationCapability capability
                && capability.isActivationActive()
                && capability.blocksThreatDuringActivation(threat);
    }

    static SunProductionCapability sunProduction(PlantBehavior behavior) {
        return behavior instanceof SunProductionCapability capability
                ? capability
                : null;
    }

    static boolean blocksVaulting(PlantBehavior behavior) {
        return behavior instanceof VaultBlockingCapability capability
                && capability.blocksVaulting();
    }

    static double armorHealth(PlantBehavior behavior) {
        return behavior instanceof PlantArmorCapability armor
                ? armor.getArmorHealth()
                : 0;
    }

    static double armorCapacity(PlantBehavior behavior) {
        return behavior instanceof PlantArmorCapability armor
                ? armor.getArmorCapacity()
                : 0;
    }

    static double modifyIncomingDamage(
            PlantBehavior behavior,
            double damage
    ) {
        return behavior instanceof DamageModifierCapability modifier
                ? modifier.modifyIncomingDamage(damage)
                : damage;
    }

    static boolean canBeEatenByZombie(PlantBehavior behavior) {
        return !(behavior instanceof ZombieEdibilityCapability capability)
                || capability.canBeEatenByZombie();
    }

    static boolean tryTriggerOnHostileContact(
            PlantBehavior behavior,
            long currentTick
    ) {
        return behavior instanceof ContactTriggerCapability capability
                && capability.tryTriggerOnHostileContact(currentTick);
    }

    static boolean updateTransientEffectIfActive(
            PlantBehavior behavior,
            long currentTick
    ) {
        if (!(behavior instanceof TransientEffectCapability capability)
                || !capability.isTransientEffectActive()) {
            return false;
        }

        capability.updateTransientEffect(currentTick);
        return true;
    }

    static boolean requiresTargetTile(PlantBehavior behavior) {
        return behavior instanceof TargetTilePlacementCapability;
    }

    static boolean canTargetTile(
            PlantBehavior behavior,
            PlantPlacementTarget target
    ) {
        return behavior instanceof TargetTilePlacementCapability capability
                && capability.canTarget(target);
    }

    static void notifyHit(
            PlantBehavior behavior,
            PlantHitContext context
    ) {
        if (behavior instanceof PlantHitReactionCapability reaction) {
            reaction.onPlantHit(context);
        }
    }
}
