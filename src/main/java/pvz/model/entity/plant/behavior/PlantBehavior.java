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
}
