package pvz.model.entity.plant.placement;

import java.util.Objects;
import java.util.Set;

import pvz.model.core.board.TileOverlayType;
import pvz.model.core.board.TileType;

public record PlantPlacementTarget(
        int column,
        int row,
        TileType tileType,
        Set<TileOverlayType> overlayTypes,
        boolean hasCrater,
        boolean occupiedByPlant
) {
    public PlantPlacementTarget {
        Objects.requireNonNull(tileType, "tile type cannot be null");

        overlayTypes = Set.copyOf(
                Objects.requireNonNull(
                        overlayTypes,
                        "overlay types cannot be null"
                )
        );
    }

    public boolean hasOverlay(TileOverlayType overlayType) {
        return overlayTypes.contains(
                Objects.requireNonNull(
                        overlayType,
                        "overlay type cannot be null"
                )
        );
    }
}
