package pvz.model.core.board;

import java.util.List;
import java.util.Objects;

import pvz.model.entity.plant.Plant;

final class PlantPlacementManager {

    private final BoardGrid grid;

    PlantPlacementManager(BoardGrid grid) {
        this.grid = Objects.requireNonNull(grid, "grid cannot be null");
    }

    String plant(int x, int y, Plant plant) {
        if (!grid.inBounds(x, y)) {
            return "location (" + x + ", " + y + ") is out of bounds!";
        }

        Tile tile = grid.getTile(x, y);

        if (!tile.isPlantableFor(plant)) {
            return "you can't plant " + plant.getName() + " on this tile!";
        }

        if (!tile.canStack(plant)) {
            return "tile (" + x + ", " + y + ") is already occupied!";
        }

        tile.addPlant(plant);
        return "planted " + plant.getName() + " at (" + x + ", " + y + ") successfully!";
    }

    Plant getTopPlant(int x, int y) {
        List<Plant> plants = grid.getTile(x, y).getPlants();

        if (plants.isEmpty()) {
            return null;
        }

        return plants.getLast();
    }

    boolean detachPlant(int x, int y, Plant plant) {
        Objects.requireNonNull(plant, "plant cannot be null");

        return grid.getTile(x, y).removePlant(plant);
    }
}
