package pvz.model.entity.plant.category.homing;

import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.core.GameEvents;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.behavior.capability.IntrinsicActionTimingCapability;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.projectile.homing.HomingImpact;
import pvz.model.entity.projectile.homing.HomingProjectile;
import pvz.model.entity.projectile.homing.HomingTarget;

abstract class AbstractHomingBehavior extends AbstractPlantBehavior
        implements PlantFoodCapability,
        IntrinsicActionTimingCapability {

    private final HomingProfile profile;

    private double nextActionTick;

    AbstractHomingBehavior(Plant owner, HomingProfile profile) {
        super(owner);

        this.profile = Objects.requireNonNull(
                profile,
                "homing profile cannot be null"
        );
    }

    @Override
    protected void afterPlaced() {
        nextActionTick = placedTick() + intervalTicks();
    }

    protected final HomingProfile profile() {
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
    public final boolean canStartAction(long currentTick) {
        return currentTick + 1e-9 >= nextActionTick
                && hasTarget(currentTick);
    }

    @Override
    public final void startAction(long currentTick) {
        if (!fireOnce(currentTick)) {
            return;
        }

        scheduleNextAction(currentTick);
    }

    @Override
    public boolean supportsPlantFood() {
        return true;
    }

    /**
     * @return true when the plant currently has something to shoot at, so a
     *         plant without any valid target never burns its cooldown.
     */
    protected abstract boolean hasTarget(long currentTick);

    /**
     * @return true when a projectile was actually launched.
     */
    protected abstract boolean fireOnce(long currentTick);

    protected final HomingProjectile launch(
            HomingTarget target,
            HomingImpact impact
    ) {
        HomingProjectile projectile = new HomingProjectile(
                world(),
                owner().getName() + " homing shot",
                owner().getX(),
                owner().getY(),
                target,
                impact,
                profile.projectileSpeedTilesPerSecond()
        );

        world().game().register(projectile);

        GameEvents.publish(
                owner().getName()
                        + " fired a homing shot from ("
                        + column()
                        + ", "
                        + row()
                        + ")"
        );

        return projectile;
    }

    private void scheduleNextAction(long currentTick) {
        double intervalTicks = intervalTicks();

        do {
            nextActionTick += intervalTicks;
        } while (nextActionTick <= currentTick);
    }

    private double intervalTicks() {
        return profile.actionIntervalSeconds() * Game.TICKS_PER_SECOND;
    }
}
