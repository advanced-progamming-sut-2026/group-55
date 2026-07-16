package pvz.model.core;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.PlantTag;

import java.util.List;

public class Board {
    private final int rows;
    private final int cols;
    private final Tile[][] tiles;

    public Board() {
        this(9, 5);
    }

    public Board(int cols, int rows) {
        this.rows = rows;
        this.cols = cols;
        tiles = new Tile[cols][rows];
        for (int x = 0; x < cols; x++)
            for (int y = 0; y < rows; y++)
                tiles[x][y] = new Tile(TileType.NORMAL);
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < cols && y >= 0 && y < rows;
    }

    public String plant(int x, int y, Plant plant) {
        if (!inBounds(x, y)) {
            return "location (" + x + ", " + y + ") is out of bounds!";
        }
        Tile tile = tiles[x][y];
        if (!tile.isPlantable()) {
            return "you can't plant on this tile!";
        }
        boolean tileEmpty = tile.getPlants().isEmpty();
        boolean stackAllowed = plant.hasTag(PlantTag.STACK)
                || tile.getPlants().stream().anyMatch(p -> p.hasTag(PlantTag.STACK));
        if (!tileEmpty && !stackAllowed) {
            return "tile (" + x + ", " + y + ") is already occupied!";
        }
        tile.addPlant(plant);
        return "planted " + plant.getName() + " at (" + x + ", " + y + ") successfully!";
    }

    public String pluck(int x, int y) {
        if (!inBounds(x, y)) {
            return "location (" + x + ", " + y + ") is out of bounds!";
        }
        Tile tile = tiles[x][y];
        List<Plant> plants = tile.getPlants();
        if (plants.isEmpty()) {
            return "there is no plant in tile (" + x + ", " + y + ")!";
        }
        Plant lastPlant = plants.getLast();
        tile.removePlant(lastPlant);
        return "plucked " + lastPlant.getName() + " at (" + x + ", " + y + ") successfully!";
    }

    public Tile getTile(int x, int y) { return tiles[x][y]; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
}