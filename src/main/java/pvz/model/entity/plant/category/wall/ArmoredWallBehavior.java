package pvz.model.entity.plant.category.wall;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.capability.DamageModifierCapability;
import pvz.model.entity.plant.behavior.capability.PlantArmorCapability;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;

class ArmoredWallBehavior extends WallBehavior
        implements DamageModifierCapability,
        PlantFoodCapability,
        PlantArmorCapability {

    private final double armorCapacity;
    private double armorHealth;

    ArmoredWallBehavior(
            Plant owner,
            boolean blocksVaulting,
            double armorCapacity
    ) {
        super(owner, blocksVaulting);

        if (armorCapacity <= 0) {
            throw new IllegalArgumentException(
                    "armor capacity must be positive"
            );
        }

        this.armorCapacity = armorCapacity;
    }

    @Override
    public boolean supportsPlantFood() {
        return true;
    }

    @Override
    public void applyPlantFood(
            long currentTick,
            long durationTicks
    ) {
        ensurePlaced();
        armorHealth = armorCapacity;
    }

    @Override
    public double modifyIncomingDamage(double damage) {
        if (armorHealth <= 0) {
            return damage;
        }

        double remainingDamage = damage - armorHealth;
        armorHealth = Math.max(0, armorHealth - damage);

        if (armorHealth == 0) {
            onArmorDestroyed();
        }

        return Math.max(0, remainingDamage);
    }

    @Override
    public double getArmorHealth() {
        return armorHealth;
    }

    @Override
    public double getArmorCapacity() {
        return armorCapacity;
    }

    protected void onArmorDestroyed() {
    }

    protected final boolean hasArmor() {
        return armorHealth > 0;
    }

    protected final void destroyArmor() {
        if (!hasArmor()) {
            return;
        }

        armorHealth = 0;
        onArmorDestroyed();
    }
}
