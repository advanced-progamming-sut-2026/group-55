package pvz.model.core.board;

final class BoardGrid {

    private final int rows;
    private final int columns;
    private final Tile[][] tiles;

    BoardGrid(int columns, int rows) {
        if (columns <= 0 || rows <= 0) {
            throw new IllegalArgumentException(
                    "board dimensions must be positive"
            );
        }

        this.rows = rows;
        this.columns = columns;
        this.tiles = new Tile[columns][rows];

        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                tiles[column][row] = new Tile(
                        TileType.NORMAL,
                        column + 1,
                        row + 1
                );
            }
        }
    }

    Tile getTile(int column, int row) {
        requireInBounds(column, row);
        return tiles[column - 1][row - 1];
    }

    boolean inBounds(int column, int row) {
        return column >= 1
                && column <= columns
                && row >= 1
                && row <= rows;
    }

    void requireInBounds(int column, int row) {
        if (!inBounds(column, row)) {
            throw new IndexOutOfBoundsException(
                    "location (" + column + ", " + row
                            + ") is out of bounds"
            );
        }
    }

    int xToColumnMovingLeft(double x) {
        return (int) Math.ceil(x);
    }

    int xToColumn(double x) {
        return (int) Math.floor(x) + 1;
    }

    int rows() {
        return rows;
    }

    int columns() {
        return columns;
    }
}
