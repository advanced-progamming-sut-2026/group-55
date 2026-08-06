package pvz.model.core.board;

import java.util.List;
import java.util.Objects;

import pvz.model.entity.plant.attack.ShotVector;
import pvz.model.entity.zombie.Zombie;

final class TargetingResolver {

    private final BoardGrid grid;

    TargetingResolver(BoardGrid grid) {
        this.grid = Objects.requireNonNull(
                grid,
                "grid cannot be null"
        );
    }

    boolean hasStraightTargetAhead(
            List<Zombie> zombies,
            int row,
            double fromX
    ) {
        return hasStraightTargetAhead(
                zombies,
                row,
                fromX,
                Integer.MAX_VALUE
        );
    }

    boolean hasStraightTargetAhead(
            List<Zombie> zombies,
            int row,
            double fromX,
            int rangeTiles
    ) {
        return hasStraightTarget(
                zombies,
                row,
                fromX,
                rangeTiles,
                HorizontalDirection.RIGHT
        );
    }

    boolean hasStraightTarget(
            List<Zombie> zombies,
            int row,
            double fromX,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        validateStraightTargetArguments(
                row,
                rangeTiles,
                direction
        );
        requireZombies(zombies);

        int startColumn = grid.xToColumn(fromX);
        int lastColumn = calculateLastReachableColumn(
                startColumn,
                rangeTiles,
                direction
        );

        return hasZombieInDirection(
                zombies,
                row,
                fromX,
                lastColumn,
                direction
        ) || hasBlockingTileInDirection(
                row,
                startColumn,
                lastColumn,
                direction
        );
    }

    boolean hasDirectionalTarget(
            List<Zombie> zombies,
            int startColumn,
            int startRow,
            int rangeTiles,
            ShotVector vector
    ) {
        grid.requireInBounds(startColumn, startRow);

        if (rangeTiles <= 0) {
            throw new IllegalArgumentException(
                    "range must be positive"
            );
        }

        Objects.requireNonNull(
                vector,
                "shot vector cannot be null"
        );
        requireZombies(zombies);

        if (hasZombieOnDirectionalPath(
                zombies,
                startColumn,
                startRow,
                rangeTiles,
                vector
        )) {
            return true;
        }

        return hasBlockingTileOnDirectionalPath(
                startColumn,
                startRow,
                rangeTiles,
                vector
        );
    }

    private void validateStraightTargetArguments(
            int row,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        if (row < 1 || row > grid.rows()) {
            throw new IndexOutOfBoundsException(
                    "row " + row + " is out of bounds"
            );
        }

        if (rangeTiles <= 0) {
            throw new IllegalArgumentException(
                    "range must be positive"
            );
        }

        Objects.requireNonNull(
                direction,
                "direction cannot be null"
        );
    }

    private int calculateLastReachableColumn(
            int startColumn,
            int rangeTiles,
            HorizontalDirection direction
    ) {
        if (rangeTiles == Integer.MAX_VALUE) {
            return direction == HorizontalDirection.RIGHT
                    ? grid.columns()
                    : 1;
        }

        if (direction == HorizontalDirection.RIGHT) {
            return Math.min(
                    grid.columns(),
                    startColumn + rangeTiles
            );
        }

        return Math.max(1, startColumn - rangeTiles);
    }

    private boolean hasBlockingTileInDirection(
            int row,
            int startColumn,
            int lastColumn,
            HorizontalDirection direction
    ) {
        int column = startColumn + direction.sign();

        while (isColumnBeforeOrAtEnd(
                column,
                lastColumn,
                direction
        )) {
            if (grid.getTile(column, row)
                    .blocksStraightProjectiles()) {
                return true;
            }

            column += direction.sign();
        }

        return false;
    }

    private boolean isColumnBeforeOrAtEnd(
            int column,
            int lastColumn,
            HorizontalDirection direction
    ) {
        if (direction == HorizontalDirection.RIGHT) {
            return column <= lastColumn;
        }

        return column >= lastColumn;
    }

    private boolean hasZombieInDirection(
            List<Zombie> zombies,
            int row,
            double fromX,
            int lastColumn,
            HorizontalDirection direction
    ) {
        return zombies.stream()
                .anyMatch(zombie ->
                        zombie.getTileY() == row
                                && isZombieInDirection(
                                zombie,
                                fromX,
                                lastColumn,
                                direction
                        )
                );
    }

    private boolean isZombieInDirection(
            Zombie zombie,
            double fromX,
            int lastColumn,
            HorizontalDirection direction
    ) {
        if (direction == HorizontalDirection.RIGHT) {
            return zombie.getX() >= fromX
                    && zombie.getTileX() <= lastColumn;
        }

        return zombie.getX() <= fromX
                && zombie.getTileX() >= lastColumn;
    }

    private boolean hasZombieOnDirectionalPath(
            List<Zombie> zombies,
            int startColumn,
            int startRow,
            int rangeTiles,
            ShotVector vector
    ) {
        for (Zombie zombie : zombies) {
            if (isTileOnDirectionalPath(
                    startColumn,
                    startRow,
                    zombie.getTileX(),
                    zombie.getTileY(),
                    rangeTiles,
                    vector
            )) {
                return true;
            }
        }

        return false;
    }

    private void requireZombies(List<Zombie> zombies) {
        Objects.requireNonNull(
                zombies,
                "zombies cannot be null"
        );
    }

    private boolean hasBlockingTileOnDirectionalPath(
            int startColumn,
            int startRow,
            int rangeTiles,
            ShotVector vector
    ) {
        for (int column = 1;
             column <= grid.columns();
             column++) {
            for (int row = 1;
                 row <= grid.rows();
                 row++) {
                if (grid.getTile(column, row)
                        .blocksStraightProjectiles()
                        && isTileOnDirectionalPath(
                        startColumn,
                        startRow,
                        column,
                        row,
                        rangeTiles,
                        vector
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isTileOnDirectionalPath(
            int startColumn,
            int startRow,
            int targetColumn,
            int targetRow,
            int rangeTiles,
            ShotVector vector
    ) {
        int columnDifference = targetColumn - startColumn;
        int rowDifference = targetRow - startRow;

        if (!vector.reachesTile(
                columnDifference,
                rowDifference
        )) {
            return false;
        }

        return rangeTiles == Integer.MAX_VALUE
                || Math.hypot(
                columnDifference,
                rowDifference
        ) <= rangeTiles;
    }
}
