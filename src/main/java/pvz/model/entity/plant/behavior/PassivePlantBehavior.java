package pvz.model.entity.plant.behavior;

import pvz.model.entity.plant.Plant;

public final class PassivePlantBehavior
        extends AbstractPlantBehavior {

    public PassivePlantBehavior(Plant owner) {
        super(owner);
    }

    @Override
    public boolean hasOngoingAction() {
        return false;
    }

    @Override
    public void updateOngoingAction(long currentTick) {
    }

    @Override
    public boolean canStartAction(long currentTick) {
        return false;
    }

    @Override
    public void startAction(long currentTick) {
    }
}
