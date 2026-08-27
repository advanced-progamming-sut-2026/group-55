package pvz.model.entity.plant.category.explosive;

import pvz.model.core.board.TileOverlayType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.capability.TargetTilePlacementCapability;
import pvz.model.entity.plant.placement.PlantPlacementTarget;

final class HotPotatoBehavior extends AbstractExplosiveBehavior
        implements TargetTilePlacementCapability {

    HotPotatoBehavior(Plant owner, ExplosiveProfile profile) {
        super(owner, profile);
    }

    @Override
    public boolean canTarget(PlantPlacementTarget target) {
        return target != null
                && target.hasOverlay(TileOverlayType.FROZEN);
    }

    @Override
    protected void afterPlaced() {
        triggerEffect(placedTick());
    }

    @Override
    protected void applyEffect(long currentTick) {
        world().board().destroyOverlay(
                column(),
                row(),
                TileOverlayType.FROZEN
        );

        publishEffect("melted the ice of its tile.");
    }
}
