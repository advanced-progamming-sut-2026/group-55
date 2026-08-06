package pvz.model.core.board;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import pvz.model.core.Updatable;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.Zombie;

public final class Board implements Updatable {

    private final BoardGrid grid;
    private final PlantPlacementManager plantPlacementManager;
    private final TargetingResolver targetingResolver;
    private final ProjectileResolver projectileResolver;
    private final TerrainManager terrainManager;
    private final AreaDamageResolver areaDamageResolver;
    private final AreaStatusEffectResolver areaStatusEffectResolver;

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
        return plantPlacementManager.plant(x, y, plant);
    }

    public Plant getTopPlant(int x, int y) {
        return plantPlacementManager.getTopPlant(x, y);
    }

    public boolean detachPlant(int x, int y, Plant plant) {
        return plantPlacementManager.detachPlant(x, y, plant);
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
        areaDamageResolver.damageZombies(zombies, centerX, centerY, radius, damage);
    }

    public void damageZombiesInArea(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            double baseDamage,
            ProjectileType projectileType
    ) {
        Objects.requireNonNull(projectileType, "projectile type cannot be null");
        double finalDamage = projectileType.calculateDamage(baseDamage);
        damageZombiesInArea(zombies, centerX, centerY, radius, finalDamage);
    }

    public void damagePlantsInArea(int centerX, int centerY, int radius, double damage) {
        areaDamageResolver.damagePlants(centerX, centerY, radius, damage);
    }

    public void damageTilesInArea(int centerX, int centerY, int radius, double damage) {
        areaDamageResolver.damageTiles(centerX, centerY, radius, damage);
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

    public boolean placeTombstone(int column, int row) {
        return terrainManager.placeTombstone(column, row);
    }

    public boolean damageTerrain(int x, int y, double damage) {
        return terrainManager.damageTerrain(x, y, damage);
    }

    public int shiftRowForSlipperyTile(int x, int y, int currentRow) {
        return terrainManager.shiftRowForSlipperyTile(x, y, currentRow);
    }

    @Override
    public void update(long tick) {
        terrainManager.update(tick);
    }
}
