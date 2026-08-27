package pvz.model.entity.plant.behavior.capability;

public interface ContactTriggerCapability {

    boolean tryTriggerOnHostileContact(long currentTick);
}
