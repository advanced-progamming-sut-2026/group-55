package pvz.model.entity.plant;

import pvz.model.entity.plant.behavior.PlantBehavior;
import pvz.model.entity.plant.behavior.capability.DamageModifierCapability;
import pvz.model.entity.plant.behavior.capability.PlantArmorCapability;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.plant.behavior.capability.PlantHitReactionCapability;
import pvz.model.entity.plant.behavior.capability.SunProductionCapability;
import pvz.model.entity.plant.behavior.capability.VaultBlockingCapability;
import pvz.model.entity.plant.hit.PlantHitContext;

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

    static void notifyHit(
            PlantBehavior behavior,
            PlantHitContext context
    ) {
        if (behavior instanceof PlantHitReactionCapability reaction) {
            reaction.onPlantHit(context);
        }
    }
}
