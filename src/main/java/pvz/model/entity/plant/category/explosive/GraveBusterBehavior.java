package pvz.model.entity.plant.category.explosive;

import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.capability.TargetTilePlacementCapability;
import pvz.model.entity.plant.placement.PlantPlacementTarget;

final class GraveBusterBehavior extends AbstractExplosiveBehavior
        implements TargetTilePlacementCapability {

    GraveBusterBehavior(Plant owner, ExplosiveProfile profile) {
        super(owner, profile);
    }

    @Override
    public boolean canTarget(PlantPlacementTarget target) {
        return target != null
                && target.tileType() == TileType.TOMBSTONE;
    }

    @Override
    protected void afterPlaced() {
        triggerEffect(placedTick());
    }

    @Override
    protected void applyEffect(long currentTick) {
        double tombstoneHealth = world().board()
                .getTile(column(), row())
                .getHealth();

        if (tombstoneHealth > 0) {
            world().board().damageTerrain(
                    column(),
                    row(),
                    tombstoneHealth
            );
        }

        publishEffect("destroyed the tombstone.");
    }
}
