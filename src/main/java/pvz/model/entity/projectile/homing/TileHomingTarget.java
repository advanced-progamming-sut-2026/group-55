package pvz.model.entity.projectile.homing;

import java.util.Objects;

import pvz.model.core.World;

public final class TileHomingTarget implements HomingTarget {

    private final World world;

    private final int column;

    private final int row;

    public TileHomingTarget(World world, int column, int row) {
        this.world = Objects.requireNonNull(world, "world cannot be null");

        if (!world.board().inBounds(column, row)) {
            throw new IllegalArgumentException(
                    "homing tile target is out of bounds"
            );
        }

        this.column = column;
        this.row = row;
    }

    public int column() {
        return column;
    }

    public int row() {
        return row;
    }

    @Override
    public double getX() {
        return column - 0.5;
    }

    @Override
    public double getY() {
        return row - 0.5;
    }

    @Override
    public boolean isValid() {
        return world.board()
                .getTile(column, row)
                .hasDestructibleContent();
    }

    @Override
    public void applyImpact(HomingImpact impact, long currentTick) {
        Objects.requireNonNull(impact, "impact cannot be null");

        impact.hitTile(world, column, row, currentTick);
    }
}
