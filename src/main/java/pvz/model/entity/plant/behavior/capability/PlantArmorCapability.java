package pvz.model.entity.plant.behavior.capability;

public interface PlantArmorCapability {

    double getArmorHealth();

    double getArmorCapacity();

    default boolean hasIntactArmor() {
        return getArmorHealth() > 0;
    }
}
