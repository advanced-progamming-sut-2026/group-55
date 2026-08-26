package pvz.model.entity.plant.behavior.capability;

/**
 * Marks a behavior that owns its action cadence instead of using the generic
 * Plant action interval gate.
 */
public interface IntrinsicActionTimingCapability {

    default boolean usesIntrinsicActionTiming() {
        return true;
    }
}
