package pvz.model.core.board;

import java.util.Objects;

import pvz.model.core.Game;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantTag;
import pvz.model.entity.plant.hit.PlantHitSource;
import pvz.model.entity.plant.lifecycle.PlantThreat;
import pvz.model.entity.projectile.ProjectileType;

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

    void placeCrater(
            int column,
            int row,
            long currentTick,
            long durationTicks
    ) {
        grid.requireInBounds(column, row);

        grid.getTile(column, row).placeCrater(currentTick, durationTicks);
    }

    boolean hasCrater(int column, int row) {
        grid.requireInBounds(column, row);

        return grid.getTile(column, row).hasCrater();
    }

    boolean destroyOverlay(
            int column,
            int row,
            TileOverlayType overlayType
    ) {
        grid.requireInBounds(column, row);

        return grid.getTile(column, row).destroyOverlay(overlayType);
    }

    int destroyOverlaysInRow(int row, TileOverlayType overlayType) {
        grid.requireInBounds(1, row);

        int destroyed = 0;

        for (int column = 1; column <= grid.columns(); column++) {
            destroyed += grid.getTile(column, row)
                    .destroyAllOverlays(overlayType);
        }

        return destroyed;
    }

    boolean damageTerrain(int x, int y, double damage) {
        return grid.getTile(x, y).takeDamage(damage);
    }

    boolean damageAllDestructibleContent(
            int x,
            int y,
            double damage
    ) {
        return grid.getTile(x, y)
                .damageAllDestructibleContent(damage);
    }

    boolean damageTerrainWithProjectile(
            int x,
            int y,
            double baseDamage,
            ProjectileType projectileType
    ) {
        return ElementInteractionResolver.damageTile(
                grid.getTile(x, y),
                projectileType,
                baseDamage
        );
    }

    boolean addPlantFreezeLevel(Plant plant, int fullFreezeLevel) {
        Tile tile = requirePlantTile(plant);

        if (tile.hasOverlay(TileOverlayType.FROZEN, plant)
                || !plant.addFreezeLevel(fullFreezeLevel)) {
            return false;
        }

        if (plant.getFreezeLevel() >= fullFreezeLevel) {
            tile.addOverlay(TileOverlayType.FROZEN, plant);
        }

        return true;
    }

    boolean coverPlantWithOctopus(Plant plant) {
        if (!plant.canBeAffectedBy(PlantThreat.OCTOPUS)) {
            return false;
        }
        return requirePlantTile(plant).addOverlay(
                TileOverlayType.OCTOPUS,
                plant
        );
    }

    boolean isPlantCovered(Plant plant) {
        return requirePlantTile(plant).blocksActionsFor(plant);
    }

    void hitPlantWithReflectedProjectile(
            Plant plant,
            ProjectileType projectileType,
            double calculatedDamage
    ) {
        Tile tile = requirePlantTile(plant);

        if (tile.hasBlockingOverlay()) {
            ElementInteractionResolver.damageTileWithCalculatedDamage(
                    tile,
                    projectileType,
                    calculatedDamage
            );
            return;
        }

        plant.receiveHit(
                PlantHitSource.PROJECTILE,
                null,
                calculatedDamage
        );
        if (plant.isRemovedFromWorld()) {
            return;
        }

        if (projectileType == ProjectileType.ICE) {
            addPlantFreezeLevel(plant, Plant.FULL_FREEZE_LEVEL);
        } else if (projectileType == ProjectileType.FIRE) {
            plant.clearFreezeLevels();
        }
    }

    int shiftRowForSlipperyTile(int x, int y, int currentRow) {
        int shiftedRow = currentRow
                + grid.getTile(x, y).getType().getLaneShift();

        return Math.max(1, Math.min(grid.rows(), shiftedRow));
    }

    void update(long tick) {
        updateCraters(tick);

        if (tick % Game.TICKS_PER_SECOND != 0) {
            return;
        }

        damageFrozenTilesNearFirePlants();
    }

    private void updateCraters(long tick) {
        for (int x = 1; x <= grid.columns(); x++) {
            for (int y = 1; y <= grid.rows(); y++) {
                grid.getTile(x, y).updateCrater(tick);
            }
        }
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

        if (!tile.hasOverlay(TileOverlayType.FROZEN)) {
            return;
        }

        int firePlantCount = countAdjacentFirePlants(x, y);

        if (firePlantCount > 0) {
            tile.damageOverlay(
                    TileOverlayType.FROZEN,
                    ADJACENT_FIRE_DAMAGE_PER_SECOND * firePlantCount
            );
        }
    }

    private Tile requirePlantTile(Plant plant) {
        Objects.requireNonNull(plant, "plant cannot be null");
        Tile tile = grid.getTile(plant.getTileX(), plant.getTileY());
        if (!tile.getPlants().contains(plant)) {
            throw new IllegalArgumentException(
                    "plant is not placed on its reported tile"
            );
        }
        return tile;
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
