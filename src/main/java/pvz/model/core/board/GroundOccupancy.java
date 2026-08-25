package pvz.model.core.board;

@FunctionalInterface
public interface GroundOccupancy {
    boolean isOccupied(int column, int row);

    static GroundOccupancy none() {
        return (column, row) -> false;
    }
}
