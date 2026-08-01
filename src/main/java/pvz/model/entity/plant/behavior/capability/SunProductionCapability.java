package pvz.model.entity.plant.behavior.capability;

public interface SunProductionCapability {

    boolean hasPendingSuns();

    void onProducedSunRemoved();
}
