package pvz.model.entity.plant.category.wall;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.behavior.capability.VaultBlockingCapability;

class WallBehavior extends AbstractPlantBehavior
        implements VaultBlockingCapability {

    private final boolean blocksVaulting;

    WallBehavior(Plant owner, boolean blocksVaulting) {
        super(owner);
        this.blocksVaulting = blocksVaulting;
    }

    @Override
    public boolean blocksVaulting() {
        return blocksVaulting;
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
