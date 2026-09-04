package pvz.model.entity.plant.category.modifier;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.plant.behavior.capability.PlantHitReactionCapability;
import pvz.model.entity.plant.hit.PlantHitContext;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.zombie.HypnosisService;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieTransformationService;
import pvz.model.entity.plant.level.PlantUpgradeType;

final class HypnoShroomBehavior extends AbstractPlantBehavior
        implements PlantFoodCapability,
        PlantHitReactionCapability,
        HypnoShroomStateCapability {

    private HypnoShroomStage stage = HypnoShroomStage.NORMAL;

    HypnoShroomBehavior(Plant owner) {
        super(owner);
    }

    @Override
    public boolean hasOngoingAction() {
        return false;
    }

    @Override
    public void updateOngoingAction(long currentTick) {
    }

    @Override
    public boolean canStartAction(long currentTick) {
        return false;
    }

    @Override
    public void startAction(long currentTick) {
    }

    @Override
    public boolean supportsPlantFood() {
        return true;
    }

    @Override
    public boolean canReceivePlantFood(long currentTick) {
        return stage == HypnoShroomStage.NORMAL;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        stage = HypnoShroomStage.GARGANTUAR_ARMED;
    }

    @Override
    public void onPlantHit(PlantHitContext context) {
        if (!context.accepted() || !context.isBite()) {
            return;
        }

        Zombie attacker = context.attacker();

        Zombie resultingAlly = null;
        if (stage == HypnoShroomStage.GARGANTUAR_ARMED) {
            resultingAlly = ZombieTransformationService.transformToAlliedGargantuar(
                    attacker,
                    context.tick()
            );
        } else if (HypnosisService.hypnotize(attacker, context.tick())) {
            resultingAlly = attacker;
        }

        if (resultingAlly != null) {
            resultingAlly.applyAlliedCombatBuff(
                    owner().getSpec().getUpgradeValue(PlantUpgradeType.HYPNO_ALLY_HP_PERCENT_ADD),
                    owner().getSpec().getUpgradeValue(PlantUpgradeType.HYPNO_ALLY_DAMAGE_PERCENT_ADD)
            );
        }

        owner().tryRemove(PlantThreat.FORCED_REMOVAL);
    }

    @Override
    public HypnoShroomStage getStage() {
        return stage;
    }
}
