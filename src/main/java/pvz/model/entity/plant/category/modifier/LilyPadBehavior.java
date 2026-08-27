package pvz.model.entity.plant.category.modifier;

import pvz.model.core.board.Tile;
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.AbstractPlantBehavior;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;

final class LilyPadBehavior extends AbstractPlantBehavior
        implements PlantFoodCapability {

    private static final int[][] ORTHOGONAL_OFFSETS = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1}
    };

    private final int plantFoodCloneCount;

    LilyPadBehavior(Plant owner) {
        super(owner);
        plantFoodCloneCount = ModifierProfiles.lilyPadCloneCount(
                owner.getSpec()
        );
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

    @Override
    public boolean supportsPlantFood() {
        return true;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        int spawned = 0;

        for (int[] offset : ORTHOGONAL_OFFSETS) {
            if (spawned >= plantFoodCloneCount) {
                return;
            }

            int targetColumn = column() + offset[0];
            int targetRow = row() + offset[1];

            if (!isEmptyWaterTile(targetColumn, targetRow)) {
                continue;
            }

            Plant clone = world().spawnPlantFromAbility(
                    owner().getName(),
                    targetColumn,
                    targetRow
            );

            if (clone != null) {
                spawned++;
            }
        }
    }

    private boolean isEmptyWaterTile(int targetColumn, int targetRow) {
        if (!world().board().inBounds(targetColumn, targetRow)
                || world().hasPushedObstacleInTile(targetColumn, targetRow)) {
            return false;
        }

        Tile tile = world().board().getTile(targetColumn, targetRow);

        return tile.getType() == TileType.WATER
                && tile.getPlants().isEmpty()
                && tile.getOverlays().isEmpty()
                && !tile.hasCrater();
    }
}
