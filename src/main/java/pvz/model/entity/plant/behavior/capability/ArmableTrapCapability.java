package pvz.model.entity.plant.behavior.capability;

public interface ArmableTrapCapability {

    boolean isArmed(long currentTick);

    void armImmediately(long currentTick);
}
