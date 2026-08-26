package pvz.model.core.board;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import pvz.model.core.Updatable;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.DamageContext;

public final class Board implements Updatable {

    private final BoardGrid grid;
    private final PlantPlacementManager plantPlacementManager;
    private final TargetingResolver targetingResolver;
    private final ProjectileResolver projectileResolver;
    private final TerrainManager terrainManager;
    private final AreaDamageResolver areaDamageResolver;
    private final AreaStatusEffectResolver areaStatusEffectResolver;
    private GroundOccupancy groundOccupancy = GroundOccupancy.none();

    public Board(int columns, int rows) {
        grid = new BoardGrid(columns, rows);
        plantPlacementManager = new PlantPlacementManager(grid);
        targetingResolver = new TargetingResolver(grid);
        projectileResolver = new ProjectileResolver(grid);
        terrainManager = new TerrainManager(grid);
        areaDamageResolver = new AreaDamageResolver(grid);
        areaStatusEffectResolver = new AreaStatusEffectResolver(grid);
    }

    public Tile getTile(int x, int y) {
        return grid.getTile(x, y);
    }

    public void setTileType(int x, int y, TileType type) {
        terrainManager.setTileType(x, y, type);
    }

    public boolean inBounds(int x, int y) {
        return grid.inBounds(x, y);
    }

    public int getRows() {
        return grid.rows();
    }

    public int getCols() {
        return grid.columns();
    }

    public String plant(int x, int y, Plant plant) {
        if (inBounds(x, y) && groundOccupancy.isOccupied(x, y)) {
            return "tile (" + x + ", " + y
                    + ") is occupied by a ground obstacle!";
        }
        return plantPlacementManager.plant(x, y, plant);
    }

    public Plant getTopPlant(int x, int y) {
        return plantPlacementManager.getTopPlant(x, y);
    }

    public boolean detachPlant(int x, int y, Plant plant) {
        return plantPlacementManager.detachPlant(x, y, plant);
    }

    public boolean movePlant(
            int fromX,
            int fromY,
            int toX,
            int toY,
            Plant plant
    ) {
        if (inBounds(toX, toY)
                && groundOccupancy.isOccupied(toX, toY)) {
            return false;
        }
        return plantPlacementManager.movePlant(
                fromX, fromY, toX, toY, plant
        );
    }

    public boolean hasStraightTargetAhead(List<Zombie> zombies, int row, double fromX) {
        return targetingResolver.hasStraightTargetAhead(zombies, row, fromX);
    }

    public boolean hasStraightTargetAhead(
            List<Zombie> zombies,
            int row,
            double fromX,
            int rangeTiles
    ) {
        return targetingResolver.hasStraightTargetAhead(zombies, row, fromX, rangeTiles);
    }

    public boolean hasStraightTarget(
            List<Zombie> zombies,
            int row,
            double fromX,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        return targetingResolver.hasStraightTarget(zombies, row, fromX, rangeTiles, direction);
    }

    public boolean hasDirectionalTarget(
            List<Zombie> zombies,
            int startColumn,
            int startRow,
            int rangeTiles,
            ShotVector vector
    ) {
        return targetingResolver.hasDirectionalTarget(zombies, startColumn, startRow, rangeTiles, vector);
    }

    public Integer findHitBlockingTile(int row, double fromX, double toX) {
        return projectileResolver.findHitBlockingTile(row, fromX, toX);
    }

    public Zombie findHitZombie(List<Zombie> zombies, int row, double fromX, double toX) {
        return findHitZombie(zombies, row, fromX, toX, Set.of());
    }

    public Zombie findHitZombie(
            List<Zombie> zombies,
            int row,
            double fromX,
            double toX,
            Set<Zombie> ignoredZombies
    ) {
        return projectileResolver.findHitZombie(zombies, row, fromX, toX, ignoredZombies);
    }

    public void damageZombiesInArea(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            double damage
    ) {
        areaDamageResolver.damageZombiesWithAbility(
                zombies,
                centerX,
                centerY,
                radius,
                damage
        );
    }

    public void damageZombiesDirectlyInArea(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            double damage
    ) {
        areaDamageResolver.damageZombiesDirectly(
                zombies,
                centerX,
                centerY,
                radius,
                damage
        );
    }

    public void damageZombiesWithProjectileInArea(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            double damage,
            ProjectileType projectileType,
            DamageContext.AttackDelivery delivery,
            long currentTick
    ) {
        areaDamageResolver.damageZombiesWithProjectile(
                zombies,
                centerX,
                centerY,
                radius,
                damage,
                projectileType,
                delivery,
                currentTick
        );
    }

    public void damagePlantsInArea(int centerX, int centerY, int radius, double damage) {
        areaDamageResolver.damagePlants(centerX, centerY, radius, damage);
    }

    public void damageTilesInArea(int centerX, int centerY, int radius, double damage) {
        areaDamageResolver.damageTiles(centerX, centerY, radius, damage);
    }

    public void damageTilesWithProjectileInArea(
            int centerX,
            int centerY,
            int radius,
            double baseDamage,
            ProjectileType projectileType
    ) {
        areaDamageResolver.damageTilesWithProjectile(
                centerX,
                centerY,
                radius,
                baseDamage,
                projectileType
        );
    }

    public void chillZombiesInArea(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            long currentTick,
            long durationTicks
    ) {
        areaStatusEffectResolver.chillZombies(
                zombies,
                centerX,
                centerY,
                radius,
                currentTick,
                durationTicks
        );
    }

    public void freezeZombiesInArea(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            long currentTick,
            long durationTicks
    ) {
        areaStatusEffectResolver.freezeZombies(
                zombies,
                centerX,
                centerY,
                radius,
                currentTick,
                durationTicks
        );
    }

    public void freezeZombiesInRow(
            List<Zombie> zombies,
            int row,
            long currentTick,
            long durationTicks
    ) {
        areaStatusEffectResolver.freezeZombiesInRow(
                zombies,
                row,
                currentTick,
                durationTicks
        );
    }

    public void clearColdEffectsFromZombiesInRow(
            List<Zombie> zombies,
            int row,
            long currentTick
    ) {
        areaStatusEffectResolver.clearColdEffectsInRow(
                zombies,
                row,
                currentTick
        );
    }

    public boolean placeTombstone(int column, int row) {
        if (inBounds(column, row)
                && groundOccupancy.isOccupied(column, row)) {
            return false;
        }
        return terrainManager.placeTombstone(column, row);
    }

    public void setGroundOccupancy(GroundOccupancy groundOccupancy) {
        this.groundOccupancy = Objects.requireNonNull(
                groundOccupancy,
                "ground occupancy cannot be null"
        );
    }

    public void placeCrater(
            int column,
            int row,
            long currentTick,
            long durationTicks
    ) {
        terrainManager.placeCrater(column, row, currentTick, durationTicks);
    }

    public boolean hasCrater(int column, int row) {
        return terrainManager.hasCrater(column, row);
    }

    public boolean destroyOverlay(
            int column,
            int row,
            TileOverlayType overlayType
    ) {
        return terrainManager.destroyOverlay(column, row, overlayType);
    }

    public int destroyOverlaysInRow(
            int row,
            TileOverlayType overlayType
    ) {
        return terrainManager.destroyOverlaysInRow(row, overlayType);
    }

    public boolean damageTerrain(int x, int y, double damage) {
        return terrainManager.damageTerrain(x, y, damage);
    }

    public boolean damageAllDestructibleContent(
            int x,
            int y,
            double damage
    ) {
        return terrainManager.damageAllDestructibleContent(
                x,
                y,
                damage
        );
    }

    public boolean damageTerrainWithProjectile(
            int x,
            int y,
            double baseDamage,
            ProjectileType projectileType
    ) {
        return terrainManager.damageTerrainWithProjectile(
                x,
                y,
                baseDamage,
                projectileType
        );
    }

    public boolean addPlantFreezeLevel(Plant plant, int fullFreezeLevel) {
        return terrainManager.addPlantFreezeLevel(plant, fullFreezeLevel);
    }

    public boolean coverPlantWithOctopus(Plant plant) {
        return terrainManager.coverPlantWithOctopus(plant);
    }

    public boolean isPlantCovered(Plant plant) {
        return terrainManager.isPlantCovered(plant);
    }

    public void hitPlantWithReflectedProjectile(
            Plant plant,
            ProjectileType projectileType,
            double calculatedDamage
    ) {
        terrainManager.hitPlantWithReflectedProjectile(
                plant,
                projectileType,
                calculatedDamage
        );
    }

    public int shiftRowForSlipperyTile(int x, int y, int currentRow) {
        return terrainManager.shiftRowForSlipperyTile(x, y, currentRow);
    }

    @Override
    public void update(long tick) {
        terrainManager.update(tick);
    }
}
