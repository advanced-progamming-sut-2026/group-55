package pvz.model.entity.plant.behavior;

import pvz.model.entity.plant.lifecycle.PlantThreat;

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

    default void onRemoved(PlantThreat threat) {
    }
}
