package pvz.model.entity.plant.behavior;

public interface PlantBehavior {

    void onPlaced(PlantPlacementContext context);

    boolean hasOngoingAction();

    void updateOngoingAction(
            long currentTick
    );

    boolean canStartAction(
            long currentTick
    );

    void startAction(
            long currentTick
    );

    default void onPlantFoodStarted(
            long currentTick,
            long durationTicks
    ) {
    }

    default void applyPlantFood(
            long currentTick,
            long durationTicks
    ) {
        throw new IllegalStateException(//TODO: in baiad eslah beshe shaiad giahi kolla plantfood handle nemikard
                "plant behavior does not support plant food"
        );
    }

    default boolean hasPendingSuns() {
        return false;
    }

    default void onProducedSunRemoved() {
    }
}
