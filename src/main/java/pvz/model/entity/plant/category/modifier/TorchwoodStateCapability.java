package pvz.model.entity.plant.category.modifier;

public interface TorchwoodStateCapability {

    TorchwoodStage getStage();

    default boolean isBlueFlameActive() {
        return getStage() == TorchwoodStage.BLUE_FLAME;
    }
}
