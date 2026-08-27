package pvz.model.entity.plant.category.wall;

import java.util.List;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.plant.behavior.capability.PlantHitReactionCapability;
import pvz.model.entity.plant.hit.PlantHitContext;
import pvz.model.entity.zombie.Zombie;

final class GarlicBehavior extends WallBehavior
        implements PlantFoodCapability,
        PlantHitReactionCapability {

    GarlicBehavior(Plant owner, boolean blocksVaulting) {
        super(owner, blocksVaulting);
    }

    @Override
    public boolean supportsPlantFood() {
        return true;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        ensurePlaced();

        int row = row();

        for (Zombie zombie : List.copyOf(world().getHostileZombies())) {
            if (zombie.isDead() || zombie.getRow() != row) {
                continue;
            }

            ZombieLaneMover.moveToAdjacentRow(world(), zombie, row);
        }
    }

    @Override
    public void onPlantHit(PlantHitContext context) {
        if (!context.isBite()) {
            return;
        }

        ZombieLaneMover.moveToAdjacentRow(
                world(),
                context.attacker(),
                row()
        );
    }
}
