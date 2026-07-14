package pvz.model.core;

public class Board {
    private final int rows;
    private final int cols;
    private final Tile[][] tiles;

    public Board() {
        this(5, 9);
    }

    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        tiles = new Tile[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                tiles[r][c] = new Tile(TileType.NORMAL);
    }

    public boolean inBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public Tile getTile(int row, int col) { return tiles[row][col]; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
}