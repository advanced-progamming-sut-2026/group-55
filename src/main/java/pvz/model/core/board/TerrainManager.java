package pvz.model.core.board;

import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.entity.plant.PlantTag;

final class TerrainManager {

    private static final double ADJACENT_FIRE_DAMAGE_PER_SECOND = 60;

    private final BoardGrid grid;

    TerrainManager(BoardGrid grid) {
        this.grid = Objects.requireNonNull(grid, "grid cannot be null");
    }

    void setTileType(int x, int y, TileType type) {
        grid.getTile(x, y).setType(
                Objects.requireNonNull(type, "tile type cannot be null")
        );
    }

    boolean placeTombstone(int column, int row) {
        if (!grid.inBounds(column, row)) {
            return false;
        }

        Tile tile = grid.getTile(column, row);

        if (tile.getType() != TileType.NORMAL
                || !tile.getPlants().isEmpty()) {
            return false;
        }

        setTileType(column, row, TileType.TOMBSTONE);
        return true;
    }

    boolean damageTerrain(int x, int y, double damage) {
        return grid.getTile(x, y).takeDamage(damage);
    }

    int shiftRowForSlipperyTile(int x, int y, int currentRow) {
        int shiftedRow = currentRow
                + grid.getTile(x, y).getType().getLaneShift();

        return Math.max(1, Math.min(grid.rows(), shiftedRow));
    }

    void update(long tick) {
        if (tick % Game.TICKS_PER_SECOND != 0) {
            return;
        }

        damageFrozenTilesNearFirePlants();
    }

    private void damageFrozenTilesNearFirePlants() {
        for (int x = 1; x <= grid.columns(); x++) {
            for (int y = 1; y <= grid.rows(); y++) {
                damageFrozenTileNearFirePlants(x, y);
            }
        }
    }

    private void damageFrozenTileNearFirePlants(int x, int y) {
        Tile tile = grid.getTile(x, y);

        if (tile.getType() != TileType.FROZEN) {
            return;
        }

        int firePlantCount = countAdjacentFirePlants(x, y);

        if (firePlantCount > 0) {
            tile.applyFireDamage(
                    ADJACENT_FIRE_DAMAGE_PER_SECOND * firePlantCount
            );
        }
    }

    private int countAdjacentFirePlants(int centerX, int centerY) {
        int count = 0;

        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int y = centerY - 1; y <= centerY + 1; y++) {
                if ((x == centerX && y == centerY)
                        || !grid.inBounds(x, y)) {
                    continue;
                }

                count += grid.getTile(x, y)
                        .countPlantsWithTag(PlantTag.FIRE);
            }
        }

        return count;
    }
}
