package pvz.model.core.board;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.placement.PlantPlacementTarget;

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

        if (plant.requiresTargetTile()) {
            return plantOnTargetTile(x, y, tile, plant);
        }

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

    boolean movePlant(
            int fromX,
            int fromY,
            int toX,
            int toY,
            Plant plant
    ) {
        Objects.requireNonNull(plant, "plant cannot be null");
        if (!grid.inBounds(fromX, fromY) || !grid.inBounds(toX, toY)) {
            return false;
        }
        Tile source = grid.getTile(fromX, fromY);
        Tile target = grid.getTile(toX, toY);
        if (!source.getPlants().contains(plant)
                || !target.isPlantableFor(plant)
                || !target.canStack(plant)) {
            return false;
        }
        source.removePlant(plant);
        target.addPlant(plant);
        return true;
    }

    private String plantOnTargetTile(
            int x,
            int y,
            Tile tile,
            Plant plant
    ) {
        if (!plant.canTargetTile(describeTarget(x, y, tile))) {
            return "you can't plant " + plant.getName() + " on this tile!";
        }

        tile.addTopPlant(plant);
        return "planted " + plant.getName() + " at (" + x + ", " + y + ") successfully!";
    }

    private PlantPlacementTarget describeTarget(int x, int y, Tile tile) {
        Set<TileOverlayType> overlayTypes = tile.getOverlays().stream()
                .map(TileOverlay::getType)
                .collect(Collectors.toUnmodifiableSet());

        return new PlantPlacementTarget(
                x,
                y,
                tile.getType(),
                overlayTypes,
                tile.hasCrater(),
                !tile.getPlants().isEmpty()
        );
    }
}
