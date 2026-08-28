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
        return target != null && target.hasOverlay(TileOverlayType.FROZEN);
    }

    @Override
    protected void afterPlaced() {
        triggerEffect(placedTick());
    }

    @Override
    protected void applyEffect(long currentTick) {
        int radius = profile().meltRadius();
        int melted = 0;
        for (int targetRow = row() - radius; targetRow <= row() + radius; targetRow++) {
            for (int targetColumn = column() - radius;
                    targetColumn <= column() + radius; targetColumn++) {
                if (world().board().inBounds(targetColumn, targetRow)
                        && world().board().destroyOverlay(
                        targetColumn, targetRow, TileOverlayType.FROZEN)) {
                    melted++;
                }
            }
        }
        if (profile().finishExplosionDamage() > 0) {
            world().damageEnemyContentsInArea(
                    column(), row(), 1, profile().finishExplosionDamage());
        }
        publishEffect("melted " + melted + " frozen tile(s)." +
                (profile().finishExplosionDamage() > 0 ? " It exploded on finish." : ""));
    }
}
