package pvz.model.entity.plant.category.explosive;

import java.util.Objects;

import pvz.model.core.GameEvents;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.behavior.capability.PlantActivationCapability;
import pvz.model.entity.plant.behavior.capability.TransientEffectCapability;
import pvz.model.entity.plant.behavior.capability.ZombieEdibilityCapability;
import pvz.model.entity.plant.lifecycle.PlantThreat;

abstract class AbstractExplosiveBehavior extends AbstractPlantBehavior
        implements ZombieEdibilityCapability,
        TransientEffectCapability,
        PlantActivationCapability {

    private final ExplosiveProfile profile;

    private final TransientActionWindow window;

    private boolean effectResolved;
    private int resolvedActivations;

    protected AbstractExplosiveBehavior(
            Plant owner,
            ExplosiveProfile profile
    ) {
        super(owner);

        this.profile = Objects.requireNonNull(
                profile,
                "explosive profile cannot be null"
        );

        this.window = new TransientActionWindow(
                profile.effectDisplayTicks()
        );
    }

    protected final ExplosiveProfile profile() {
        return profile;
    }

    public final TransientActionWindow window() {
        return window;
    }

    @Override
    public boolean canBeEatenByZombie() {
        return edibleWhileIdle();
    }

    protected boolean edibleWhileIdle() {
        return true;
    }

    @Override
    public boolean hasOngoingAction() {
        return true;
    }

    @Override
    public void updateOngoingAction(long currentTick) {
        if (window.isEffectActive()) {
            updateTransientEffect(currentTick);
            return;
        }

        if (window.isFinished()) {
            return;
        }

        onIdleTick(currentTick);
    }

    @Override
    public final boolean isTransientEffectActive() {
        return window.isEffectActive();
    }

    @Override
    public final boolean isActivationActive() {
        return window.isEffectActive();
    }

    @Override
    public final void updateTransientEffect(long currentTick) {
        if (!window.shouldFinish(currentTick)) {
            return;
        }

        window.finish();
        resolveEffect(currentTick);
        if (resolvedActivations < profile.maxActivations()) {
            effectResolved = false;
            window.reset();
            return;
        }
        owner().tryRemove(PlantThreat.TRANSIENT_EFFECT_COMPLETION);
    }

    protected void onIdleTick(long currentTick) {
    }

    @Override
    public boolean canStartAction(long currentTick) {
        return false;
    }

    @Override
    public void startAction(long currentTick) {
    }

    protected final boolean triggerEffect(long currentTick) {
        ensurePlaced();

        if (!window.start(currentTick)) {
            return false;
        }

        if (resolvesEffectImmediately()) {
            resolveEffect(currentTick);
        }

        return true;
    }

    protected final boolean canTrigger() {
        return window.getState() == TransientActionWindow.State.IDLE;
    }

    protected boolean resolvesEffectImmediately() {
        return false;
    }

    private void resolveEffect(long currentTick) {
        if (effectResolved) {
            return;
        }

        effectResolved = true;
        resolvedActivations++;
        applyEffect(currentTick);
    }

    protected final void publishEffect(String description) {
        GameEvents.publish(
                owner().getName()
                        + " at ("
                        + column()
                        + ", "
                        + row()
                        + ") "
                        + description
        );
    }

    protected abstract void applyEffect(long currentTick);
}
