package pvz.model.entity.plant.behavior;

import java.util.Objects;

import pvz.model.core.World;
import pvz.model.entity.plant.Plant;

public abstract class AbstractPlantBehavior
        implements PlantBehavior {

    private final Plant owner;

    private PlantPlacementContext placementContext;

    protected AbstractPlantBehavior(Plant owner) {
        this.owner = Objects.requireNonNull(owner, "owner plant cannot be null");
    }

    @Override
    public final void onPlaced(PlantPlacementContext context) {
        Objects.requireNonNull(context, "placement context cannot be null");

        if (placementContext != null) {
            throw new IllegalStateException("plant behavior is already placed");
        }

        if (context.owner() != owner) {
            throw new IllegalArgumentException("placement context belongs to another plant");
        }

        placementContext = context;

        try {
            afterPlaced();
        } catch (RuntimeException | Error exception) {
            placementContext = null;
            throw exception;
        }
    }

    protected void afterPlaced() {
    }

    protected final Plant owner() {
        return owner;
    }

    protected final World world() {
        return placementContext().world();
    }

    protected final int column() {
        return placementContext().column();
    }

    protected final int row() {
        return placementContext().row();
    }

    protected final long placedTick() {
        return placementContext().placedTick();
    }

    protected final void ensurePlaced() {
        placementContext();
    }

    private PlantPlacementContext placementContext() {
        if (placementContext == null) {
            throw new IllegalStateException(
                    "plant behavior must be placed before use"
            );
        }

        return placementContext;
    }
}
