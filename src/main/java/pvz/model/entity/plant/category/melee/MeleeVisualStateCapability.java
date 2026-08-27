package pvz.model.entity.plant.category.melee;

public interface MeleeVisualStateCapability {

    long getLastAttackTick();

    MeleeAttackDirection getLastAttackDirection();

    default boolean isDigesting(long currentTick) {
        return false;
    }

    default int getGrowthStage(long currentTick) {
        return 1;
    }
}
