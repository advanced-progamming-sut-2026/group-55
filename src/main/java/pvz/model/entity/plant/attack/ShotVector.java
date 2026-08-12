package pvz.model.entity.plant.attack;

import pvz.model.core.board.HorizontalDirection;

public record ShotVector(
        int columnStep,
        int rowStep
) {
    public static final ShotVector RIGHT = new ShotVector(1, 0);
    public static final ShotVector LEFT = new ShotVector(-1, 0);
    public static final ShotVector UP = new ShotVector(0, -1);
    public static final ShotVector DOWN = new ShotVector(0, 1);

    public static final ShotVector UP_RIGHT = new ShotVector(1, -1);
    public static final ShotVector DOWN_RIGHT = new ShotVector(1, 1);
    public static final ShotVector UP_LEFT = new ShotVector(-1, -1);
    public static final ShotVector DOWN_LEFT = new ShotVector(-1, 1);

    public static final ShotVector SHALLOW_UP_RIGHT = new ShotVector(2, -1);
    public static final ShotVector SHALLOW_DOWN_RIGHT = new ShotVector(2, 1);

    public ShotVector {
        if (columnStep == 0 && rowStep == 0) {
            throw new IllegalArgumentException(
                    "shot vector needs at least one non-zero step"
            );
        }

        int divisor = greatestCommonDivisor(Math.abs(columnStep), Math.abs(rowStep));

        columnStep /= divisor;
        rowStep /= divisor;
    }

    public boolean isHorizontal() {
        return rowStep == 0;
    }

    public HorizontalDirection horizontalDirection() {
        if (!isHorizontal()) {
            throw new IllegalStateException(
                    "a non-horizontal shot vector has no horizontal direction"
            );
        }

        return columnStep > 0 ? HorizontalDirection.RIGHT : HorizontalDirection.LEFT;
    }

    public double unitColumnStep() {
        return columnStep / length();
    }

    public double unitRowStep() {
        return rowStep / length();
    }

    public boolean reachesTile(
            int columnDifference,
            int rowDifference
    ) {
        long crossProduct =
                (long) columnDifference * rowStep
                        - (long) rowDifference * columnStep;

        long dotProduct =
                (long) columnDifference * columnStep
                        + (long) rowDifference * rowStep;

        return crossProduct == 0 && dotProduct > 0;
    }

    private double length() {
        return Math.hypot(columnStep, rowStep);
    }

    private static int greatestCommonDivisor(int first, int second) {
        while (second != 0) {
            int remainder = first % second;
            first = second;
            second = remainder;
        }

        return Math.max(1, first);
    }
}
