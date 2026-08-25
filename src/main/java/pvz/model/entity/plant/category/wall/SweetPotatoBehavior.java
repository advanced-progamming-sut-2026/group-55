package pvz.model.entity.plant.category.wall;

import java.util.List;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.zombie.Zombie;

final class SweetPotatoBehavior extends WallBehavior
        implements PlantFoodCapability {

    private static final int ADJACENT_ROW_DISTANCE = 1;

    private static final double COLUMN_RANGE = 1;

    SweetPotatoBehavior(Plant owner, boolean blocksVaulting) {
        super(owner, blocksVaulting);
    }

    @Override
    public boolean hasOngoingAction() {
        return true;
    }

    @Override
    public void updateOngoingAction(long currentTick) {
        attractNearbyZombies();
    }

    @Override
    public boolean supportsPlantFood() {
        return true;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        attractNearbyZombies();
    }

    private void attractNearbyZombies() {
        ensurePlaced();

        int row = row();

        for (Zombie zombie : List.copyOf(world().getZombies())) {
            if (!isInAttractionRange(zombie, row)) {
                continue;
            }

            ZombieLaneMover.moveToRow(zombie, row);
        }
    }

    private boolean isInAttractionRange(Zombie zombie, int row) {
        if (zombie.isDead()) {
            return false;
        }

        if (Math.abs(zombie.getRow() - row) != ADJACENT_ROW_DISTANCE) {
            return false;
        }

        return Math.abs(zombie.getX() - owner().getX()) <= COLUMN_RANGE;
    }
}
