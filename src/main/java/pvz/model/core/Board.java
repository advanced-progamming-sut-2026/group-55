package pvz.model.core;

import java.util.List;
import java.util.Objects;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantTag;

/** A board whose public coordinates are one-based: x=1..columns, y=1..rows. */
public final class Board implements Updatable {
    public static final int DEFAULT_COLUMNS = 9;
    public static final int DEFAULT_ROWS = 5;
    private static final double ADJACENT_FIRE_DAMAGE_PER_SECOND = 60;

    private final int rows;
    private final int columns;
    private final Tile[][] tiles;

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
                tiles[x][y] = new Tile(TileType.NORMAL);
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
        if (!canStack(tile, plant)) {
            return "tile (" + x + ", " + y + ") is already occupied!";
        }
        tile.addPlant(plant);
        return "planted " + plant.getName() + " at (" + x + ", " + y + ") successfully!";
    }

    private boolean canStack(Tile tile, Plant newPlant) {
        List<Plant> currentPlants = tile.getPlants();
        if (currentPlants.isEmpty()) {
            return true;
        }
        if (tile.getType() == TileType.WATER && hasLilyPad(currentPlants)) {
            return currentPlants.size() == 1;
        }
        if (newPlant.getName().equalsIgnoreCase("Pea Pod")) {
            return currentPlants.size() < 5
                    && currentPlants.stream().allMatch(this::isPeaPod);
        }
        if (newPlant.getName().equalsIgnoreCase("Pumpkin")) {
            return currentPlants.size() == 1 && !hasPumpkin(currentPlants);
        }
        return false;
    }

    private boolean isPeaPod(Plant plant) {
        return plant.getName().equalsIgnoreCase("Pea Pod");
    }

    private boolean hasLilyPad(List<Plant> plants) {
        return plants.stream().anyMatch(plant -> plant.getName().equalsIgnoreCase("Lily Pad"));
    }

    private boolean hasPumpkin(List<Plant> plants) {
        return plants.stream().anyMatch(plant -> plant.getName().equalsIgnoreCase("Pumpkin"));
    }

    public String pluck(int x, int y) {
        if (!inBounds(x, y)) {
            return "location (" + x + ", " + y + ") is out of bounds!";
        }
        Tile tile = getTile(x, y);
        List<Plant> plants = tile.getPlants();
        if (plants.isEmpty()) {
            return "there is no plant in tile (" + x + ", " + y + ")!";
        }
        Plant lastPlant = plants.get(plants.size() - 1);
        tile.removePlant(lastPlant);
        return "plucked " + lastPlant.getName() + " at (" + x + ", " + y + ") successfully!";
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

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return columns;
    }
}
