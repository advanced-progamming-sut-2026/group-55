package pvz.model.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.Zombie;

public final class Board implements Updatable {
    public static final int DEFAULT_COLUMNS = 9;
    public static final int DEFAULT_ROWS = 5;
    private static final double ADJACENT_FIRE_DAMAGE_PER_SECOND = 60;

    private final int rows;
    private final int columns;
    private final Tile[][] tiles;
    private final List<Zombie> zombies = new ArrayList<>();

    public Board() {
        this(DEFAULT_COLUMNS, DEFAULT_ROWS);
    }

    public Board(int columns, int rows) {
        if (columns <= 0 || rows <= 0) {
            throw new IllegalArgumentException("board dimensions must be positive");
        }
        this.rows = rows;
        this.columns = columns;
        tiles = new Tile[columns][rows];
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                tiles[x][y] = new Tile(TileType.NORMAL, x + 1 , y + 1);
            }
        }
    }

    //Tile:
    public Tile getTile(int x, int y) {
        requireInBounds(x, y);
        return tiles[x - 1][y - 1];
    }

    public void setTileType(int x, int y, TileType type) {
        getTile(x, y).setType(Objects.requireNonNull(type));
    }

    public boolean inBounds(int x, int y) {
        return x >= 1 && x <= columns && y >= 1 && y <= rows;
    }

    public Integer findHitBlockingTile(
            int row,
            double fromX,
            double toX
    ) {
        if (row < 1 || row > rows) {
            throw new IndexOutOfBoundsException(
                    "row " + row + " is out of bounds"
            );
        }

        if (toX > fromX) {
            return findRightBlockingTile(
                    row,
                    fromX,
                    toX
            );
        }

        if (toX < fromX) {
            return findLeftBlockingTile(
                    row,
                    fromX,
                    toX
            );
        }

        return null;
    }

    private Integer findRightBlockingTile(
            int row,
            double fromX,
            double toX
    ) {
        int firstColumn = Math.max(
                1,
                xToColumn(fromX)
        );

        int lastColumn = Math.min(
                columns,
                xToColumn(toX)
        );

        for (int column = firstColumn;
             column <= lastColumn;
             column++) {

            if (getTile(column, row)
                    .blocksStraightProjectiles()) {
                return column;
            }
        }

        return null;
    }

    private Integer findLeftBlockingTile(
            int row,
            double fromX,
            double toX
    ) {
        int firstColumn = Math.min(
                columns,
                xToColumnMovingLeft(fromX)
        );

        int lastColumn = Math.max(
                1,
                xToColumnMovingLeft(toX)
        );

        for (int column = firstColumn;
             column >= lastColumn;
             column--) {

            if (getTile(column, row)
                    .blocksStraightProjectiles()) {
                return column;
            }
        }

        return null;
    }

    public boolean placeTombstone(int column, int row) {
        if(!inBounds(column, row)) {
            return false;
        }
        Tile  tile = getTile(column, row);
        if(tile.getType() != TileType.NORMAL || !tile.getPlants().isEmpty()) {
            return false;
        }
        setTileType(column, row, TileType.TOMBSTONE);
        return true;
    }

    private void damageFrozenTilesNearFirePlants() {
        for (int x = 1; x <= columns; x++) {
            for (int y = 1; y <= rows; y++) {
                Tile tile = getTile(x, y);
                if (tile.getType() == TileType.FROZEN && hasAdjacentFirePlant(x, y)) {
                    tile.applyFireDamage(ADJACENT_FIRE_DAMAGE_PER_SECOND);
                }
            }
        }
    }

    public boolean damageTerrain(int x, int y, double damage) {
        return getTile(x, y).takeDamage(damage);
    }

    private int calculateLastReachableColumn(
            int startColumn,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        if (rangeTiles == Integer.MAX_VALUE) {
            return direction == HorizontalDirection.RIGHT ? columns : 1;
        }

        if (direction == HorizontalDirection.RIGHT) {
            return Math.min(columns, startColumn + rangeTiles);
        }

        return Math.max(1, startColumn - rangeTiles);
    }

    private boolean hasBlockingTileInDirection(
            int row,
            int startColumn,
            int lastColumn,
            HorizontalDirection direction
    ) {
        int column = startColumn + direction.sign();

        while (isColumnBeforeOrAtEnd(column, lastColumn, direction)) {
            if (getTile(column, row)
                    .blocksStraightProjectiles()) {
                return true;
            }

            column += direction.sign();
        }

        return false;
    }

    private boolean isColumnBeforeOrAtEnd(
            int column,
            int lastColumn,
            HorizontalDirection direction
    ) {
        if (direction == HorizontalDirection.RIGHT) {
            return column <= lastColumn;
        }

        return column >= lastColumn;
    }

    //Plant:
    public String plant(int x, int y, Plant plant) {
        if (!inBounds(x, y)) {
            return "location (" + x + ", " + y + ") is out of bounds!";
        }
        Tile tile = getTile(x, y);
        if (!tile.isPlantableFor(plant)) {
            return "you can't plant " + plant.getName() + " on this tile!";
        }
        if (!tile.canStack( plant)) {
            return "tile (" + x + ", " + y + ") is already occupied!";
        }
        tile.addPlant(plant);
        return "planted " + plant.getName() + " at (" + x + ", " + y + ") successfully!";
    }

    public Plant removeTopPlant(int x, int y) {
        Tile tile = getTile(x, y);
        List<Plant> plants = tile.getPlants();
        if (plants.isEmpty()) {
            return null;
        }
        Plant lastPlant = plants.getLast();
        tile.removePlant(lastPlant);
        return lastPlant;
    }

    private boolean hasAdjacentFirePlant(int centerX, int centerY) {
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int y = centerY - 1; y <= centerY + 1; y++) {
                if ((x != centerX || y != centerY)
                        && inBounds(x, y)
                        && getTile(x, y).hasFirePlant()) {
                    return true;
                }
            }
        }
        return false;
    }
    public boolean hasStraightTargetAhead(
            int row,
            double fromX
    ) {
        return hasStraightTargetAhead(
                row,
                fromX,
                Integer.MAX_VALUE
        );
    }
    public boolean hasStraightTargetAhead(
            int row,
            double fromX,
            int rangeTiles
    ) {
        return hasStraightTarget(
                row,
                fromX,
                rangeTiles,
                HorizontalDirection.RIGHT
        );
    }
    public boolean hasStraightTarget(
            int row,
            double fromX,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        if (row < 1 || row > rows) {
            throw new IndexOutOfBoundsException(
                    "row " + row + " is out of bounds"
            );
        }

        if (rangeTiles <= 0) {
            throw new IllegalArgumentException(
                    "range must be positive"
            );
        }

        Objects.requireNonNull(
                direction,
                "direction cannot be null"
        );

        int startColumn = xToColumn(fromX);

        int lastColumn = calculateLastReachableColumn(
                startColumn,
                rangeTiles,
                direction
        );

        if (hasZombieInDirection(
                row,
                fromX,
                lastColumn,
                direction
        )) {
            return true;
        }

        return hasBlockingTileInDirection(
                row,
                startColumn,
                lastColumn,
                direction
        );
    }


    //Zombie:
    public void addZombie(Zombie zombie) {
        zombies.add(Objects.requireNonNull(zombie));
    }

    public void removeZombie(Zombie zombie) {
        zombies.remove(zombie);
    }

    public List<Zombie> getZombies() {
        return List.copyOf(zombies);
    }

    public boolean hasZombieAhead(int row, double fromX) {
        return zombies.stream()
                .anyMatch(zombie -> zombie.getTileY() == row && zombie.getX() >= fromX);
    }

    public Zombie findHitZombie(
            int row,
            double fromX,
            double toX
    ) {
        if (row < 1 || row > rows) {
            throw new IndexOutOfBoundsException(
                    "row " + row + " is out of bounds"
            );
        }

        if (toX > fromX) {
            return findRightMovingProjectileHit(
                    row,
                    fromX,
                    toX
            );
        }

        if (toX < fromX) {
            return findLeftMovingProjectileHit(
                    row,
                    fromX,
                    toX
            );
        }

        return null;
    }

    private Zombie findRightMovingProjectileHit(
            int row,
            double fromX,
            double toX
    ) {
        Zombie nearest = null;

        for (Zombie zombie : zombies) {
            if (zombie.getTileY() != row || zombie.getX() <= fromX || zombie.getX() > toX) {
                continue;
            }

            if (nearest == null || zombie.getX() < nearest.getX()) {
                nearest = zombie;
            }
        }

        return nearest;
    }

    private Zombie findLeftMovingProjectileHit(
            int row,
            double fromX,
            double toX
    ) {
        Zombie nearest = null;

        for (Zombie zombie : zombies) {
            if (zombie.getTileY() != row || zombie.getX() >= fromX || zombie.getX() < toX) {
                continue;
            }

            if (nearest == null || zombie.getX() > nearest.getX()) {
                nearest = zombie;
            }
        }

        return nearest;
    }

    private boolean hasZombieInDirection(
            int row,
            double fromX,
            int lastColumn,
            HorizontalDirection direction
    ) {
        return zombies.stream()
                .anyMatch(zombie -> zombie.getTileY() == row
                                && isZombieInDirection(
                                zombie,
                                fromX,
                                lastColumn,
                                direction
                        )
                );
    }

    private boolean isZombieInDirection(
            Zombie zombie,
            double fromX,
            int lastColumn,
            HorizontalDirection direction
    ) {
        if (direction == HorizontalDirection.RIGHT) {
            return zombie.getX() >= fromX
                    && zombie.getTileX() <= lastColumn;
        }

        return zombie.getX() <= fromX
                && zombie.getTileX() >= lastColumn;
    }

    public int shiftRowForSlipperyTile(int x, int y, int currentRow) {
        int shiftedRow = currentRow + getTile(x, y).getType().getLaneShift();
        return Math.max(1, Math.min(rows, shiftedRow));
    }

    @Override
    public void update(long tick) {
        if (tick % Game.TICKS_PER_SECOND != 0) {
            return;
        }
        damageFrozenTilesNearFirePlants();
    }
    //helpier method
    private void requireInBounds(int x, int y) {
        if (!inBounds(x, y)) {
            throw new IndexOutOfBoundsException("location (" + x + ", " + y + ") is out of bounds");
        }
    }

    private int xToColumnMovingLeft(double x) {
        return (int) Math.ceil(x);
    }

    private int xToColumn(double x) {
        return (int) Math.floor(x) + 1;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return columns;
    }
}
