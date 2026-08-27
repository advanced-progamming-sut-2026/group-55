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

        if (stage == HypnoShroomStage.GARGANTUAR_ARMED) {
            ZombieTransformationService.transformToAlliedGargantuar(
                    attacker,
                    context.tick()
            );
        } else {
            HypnosisService.hypnotize(attacker, context.tick());
        }

        owner().tryRemove(PlantThreat.FORCED_REMOVAL);
    }

    @Override
    public HypnoShroomStage getStage() {
        return stage;
    }
}
