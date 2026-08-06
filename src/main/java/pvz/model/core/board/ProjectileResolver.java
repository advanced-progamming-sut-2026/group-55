package pvz.model.core.board;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import pvz.model.entity.zombie.Zombie;

final class ProjectileResolver {

    private final BoardGrid grid;

    ProjectileResolver(BoardGrid grid) {
        this.grid = Objects.requireNonNull(
                grid,
                "grid cannot be null"
        );
    }

    Integer findHitBlockingTile(
            int row,
            double fromX,
            double toX
    ) {
        if (toX > fromX) {
            return findRightBlockingTile(row, fromX, toX);
        }

        if (toX < fromX) {
            return findLeftBlockingTile(row, fromX, toX);
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
                grid.xToColumn(fromX)
        );
        int lastColumn = Math.min(
                grid.columns(),
                grid.xToColumn(toX)
        );

        for (int column = firstColumn;
             column <= lastColumn;
             column++) {
            if (grid.getTile(column, row)
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
                grid.columns(),
                grid.xToColumnMovingLeft(fromX)
        );
        int lastColumn = Math.max(
                1,
                grid.xToColumnMovingLeft(toX)
        );

        for (int column = firstColumn;
             column >= lastColumn;
             column--) {
            if (grid.getTile(column, row)
                    .blocksStraightProjectiles()) {
                return column;
            }
        }

        return null;
    }

    Zombie findHitZombie(
            List<Zombie> zombies,
            int row,
            double fromX,
            double toX,
            Set<Zombie> ignoredZombies
    ) {
        Objects.requireNonNull(
                ignoredZombies,
                "ignored zombies cannot be null"
        );
        Objects.requireNonNull(
                zombies,
                "zombies cannot be null"
        );

        Zombie nearest = null;

        for (Zombie zombie : zombies) {
            if (ignoredZombies.contains(zombie)
                    || zombie.getTileY() != row) {
                continue;
            }

            if (isNearerRightHit(
                    zombie,
                    nearest,
                    fromX,
                    toX
            ) || isNearerLeftHit(
                    zombie,
                    nearest,
                    fromX,
                    toX
            )) {
                nearest = zombie;
            }
        }

        return nearest;
    }

    private boolean isNearerRightHit(
            Zombie zombie,
            Zombie nearest,
            double fromX,
            double toX
    ) {
        return toX > fromX
                && zombie.getX() > fromX
                && zombie.getX() <= toX
                && (nearest == null
                || zombie.getX() < nearest.getX());
    }

    private boolean isNearerLeftHit(
            Zombie zombie,
            Zombie nearest,
            double fromX,
            double toX
    ) {
        return toX < fromX
                && zombie.getX() < fromX
                && zombie.getX() >= toX
                && (nearest == null
                || zombie.getX() > nearest.getX());
    }
}
