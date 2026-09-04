package pvz.model.entity.plant.category.modifier;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.plant.behavior.capability.ProjectilePassThroughModifierCapability;
import pvz.model.entity.projectile.PeaHeatState;
import pvz.model.entity.projectile.ProjectileModifierTarget;
import pvz.model.entity.plant.level.PlantUpgradeType;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.core.GameEvents;

final class TorchwoodBehavior extends AbstractPlantBehavior
        implements PlantFoodCapability,
        ProjectilePassThroughModifierCapability,
        TorchwoodStateCapability {

    private final double normalPeaDamageMultiplier;
    private final double bluePeaDamageMultiplier;
    private final long blueFlameDurationTicks;

    private TorchwoodStage stage = TorchwoodStage.NORMAL;
    private long blueFlameUntilTick;

    TorchwoodBehavior(Plant owner) {
        super(owner);
        normalPeaDamageMultiplier = ModifierProfiles
                .torchwoodPeaDamageMultiplier(owner.getSpec());
        bluePeaDamageMultiplier = ModifierProfiles
                .torchwoodPlantFoodPeaDamageMultiplier(owner.getSpec());
        blueFlameDurationTicks = ModifierProfiles
                .torchwoodBlueFlameDurationTicks(owner.getSpec());
    }

    @Override
    public boolean hasOngoingAction() {
        return stage == TorchwoodStage.BLUE_FLAME;
    }

    @Override
    public void updateOngoingAction(long currentTick) {
        expireBlueFlameIfNeeded(currentTick);
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
    public void applyPlantFood(long currentTick, long durationTicks) {
        stage = TorchwoodStage.BLUE_FLAME;
        blueFlameUntilTick = Math.addExact(
                currentTick,
                blueFlameDurationTicks
        );
    }

    @Override
    public void modifyProjectile(ProjectileModifierTarget projectile) {
        boolean blueFlameActive = isBlueFlameActive();
        PeaHeatState targetState = blueFlameActive
                ? PeaHeatState.BLUE_FIRE
                : PeaHeatState.FIRE;
        double targetMultiplier = blueFlameActive
                ? bluePeaDamageMultiplier
                : normalPeaDamageMultiplier;

        projectile.promotePeaHeat(targetState, targetMultiplier);
    }


    @Override
    public void onRemoved(PlantThreat threat) {
        double deathDamage = owner().getSpec().getUpgradeValue(
                PlantUpgradeType.TORCHWOOD_DEATH_EXPLOSION_DAMAGE
        );
        if (deathDamage <= 0
                || (threat != PlantThreat.DAMAGE && threat != PlantThreat.INSTANT_DESTROY)) {
            return;
        }
        world().damageEnemyContentsInRow(row(), deathDamage);
        GameEvents.publish(owner().getName() + " released a row-wide death burst for "
                + deathDamage + " damage.");
    }

    @Override
    public TorchwoodStage getStage() {
        expireBlueFlameIfNeeded(world().game().getCurrentTick());
        return stage;
    }

    private void expireBlueFlameIfNeeded(long currentTick) {
        if (stage == TorchwoodStage.BLUE_FLAME
                && currentTick >= blueFlameUntilTick) {
            stage = TorchwoodStage.NORMAL;
            blueFlameUntilTick = 0;
        }
    }
}
