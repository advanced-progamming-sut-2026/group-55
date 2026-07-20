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

    public boolean inBounds(int x, int y) {
        return x >= 1 && x <= columns && y >= 1 && y <= rows;
    }

    public Tile getTile(int x, int y) {
        requireInBounds(x, y);
        return tiles[x - 1][y - 1];
    }

    public void setTileType(int x, int y, TileType type) {
        getTile(x, y).setType(Objects.requireNonNull(type));
    }

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
                .anyMatch(zombie -> zombie.getRow() == row && zombie.getX() >= fromX);
    }

    public boolean hasTileObstacleAhead(int row, int fromX) {
        for (int column = fromX + 1; column <= columns; column++) {
            if (getTile(column, row).blocksStraightProjectiles()) {
                return true;
            }
        }
        return false;
    }

    public Zombie findHitZombie(int row, double fromX, double toX) {
        if (toX <= fromX) {
            return null;
        }

        if (row < 1 || row > rows) {
            throw new IndexOutOfBoundsException("row " + row + " is out of bounds");
        }
        Zombie first = null;
        for (Zombie zombie : zombies) {
            if (zombie.getRow() == row && zombie.getX() > fromX && zombie.getX() <= toX
                    && (first == null || zombie.getX() < first.getX())) {
                first = zombie;
            }
        }
        return first;
    }

    public Integer findHitBlockingTile(int row, double fromX, double toX) {
        if (toX <= fromX) {
            return null;
        }

        if (row < 1 || row > rows) {
            throw new IndexOutOfBoundsException(
                    "row " + row + " is out of bounds"
            );
        }

        int firstColumn = Math.max(1, xToColumn(fromX));
        int lastColumn = Math.min(columns, xToColumn(toX));

        for (int column = firstColumn; column <= lastColumn; column++) {
            if (getTile(column, row).blocksStraightProjectiles()) {
                return column;
            }
        }

        return null;
    }

    public boolean damageTerrain(int x, int y, double damage) {
        return getTile(x, y).takeDamage(damage);
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

    private void requireInBounds(int x, int y) {
        if (!inBounds(x, y)) {
            throw new IndexOutOfBoundsException("location (" + x + ", " + y + ") is out of bounds");
        }
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
