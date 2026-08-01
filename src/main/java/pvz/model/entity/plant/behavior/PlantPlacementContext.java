package pvz.model.entity.plant.behavior;

import java.util.Objects;

import pvz.model.core.World;
import pvz.model.entity.plant.Plant;

public record PlantPlacementContext(
        Plant owner,
        World world,
        int column,
        int row,
        long placedTick
) {
    public PlantPlacementContext {
        owner = Objects.requireNonNull(
                owner,
                "owner plant cannot be null"
        );

        world = Objects.requireNonNull(
                world,
                "world cannot be null"
        );

        if (!world.board().inBounds(column, row)) {
            throw new IllegalArgumentException(
                    "plant position is out of bounds"
            );
        }

        if (placedTick < 0) {
            throw new IllegalArgumentException(
                    "placed tick cannot be negative"
            );
        }
    }
}
