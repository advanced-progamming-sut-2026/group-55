package pvz.model.entity.plant.category.melee;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.behavior.capability.IntrinsicActionTimingCapability;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;

abstract class AbstractMeleeBehavior extends AbstractPlantBehavior
        implements PlantFoodCapability,
        IntrinsicActionTimingCapability,
        MeleeVisualStateCapability {

    private final MeleeProfile profile;
    private double nextActionTick;
    private long lastAttackTick = -1;
    private MeleeAttackDirection lastAttackDirection;

    AbstractMeleeBehavior(Plant owner, MeleeProfile profile) {
        super(owner);
        this.profile = profile;
    }

    @Override
    protected void afterPlaced() {
        nextActionTick = placedTick()
                + profile.actionIntervalSeconds()
                * Game.TICKS_PER_SECOND;
    }

    protected final MeleeProfile profile() {
        return profile;
    }

    @Override
    public final boolean hasOngoingAction() {
        return false;
    }

    @Override
    public final void updateOngoingAction(long currentTick) {
    }

    @Override
    public boolean canStartAction(long currentTick) {
        return currentTick + 1e-9 >= nextActionTick
                && hasAttackTarget(currentTick);
    }

    @Override
    public final void startAction(long currentTick) {
        performAttack(currentTick);
        scheduleNextAction(currentTick);
    }

    protected void scheduleNextAction(long currentTick) {
        double intervalTicks = profile.actionIntervalSeconds()
                * Game.TICKS_PER_SECOND;
        do {
            nextActionTick += intervalTicks;
        } while (nextActionTick <= currentTick);
    }

    protected abstract boolean hasAttackTarget(long currentTick);

    protected abstract void performAttack(long currentTick);

    protected final void markAttack(
            long currentTick,
            MeleeAttackDirection direction
    ) {
        lastAttackTick = currentTick;
        lastAttackDirection = direction;
    }

    protected final void publishAttack(String description) {
        GameEvents.publish(owner().getName() + " " + description);
    }

    @Override
    public final long getLastAttackTick() {
        return lastAttackTick;
    }

    @Override
    public final MeleeAttackDirection getLastAttackDirection() {
        return lastAttackDirection;
    }

    @Override
    public boolean supportsPlantFood() {
        return true;
    }
}
