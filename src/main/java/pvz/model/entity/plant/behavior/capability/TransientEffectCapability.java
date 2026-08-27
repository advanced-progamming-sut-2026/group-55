package pvz.model.entity.plant.behavior.capability;

/**
 * Owns a short visual/effect window whose clock must keep advancing even when
 * the plant is otherwise prevented from acting.
 */
public interface TransientEffectCapability {

    boolean isTransientEffectActive();

    void updateTransientEffect(long currentTick);
}
