package pvz.model.core.board;

import java.util.Objects;

/** Future shared explosion/area damage entry point for plants and world events. */
public final class AreaDamageResolver {
    private final Board board;

    public AreaDamageResolver(Board board) {
        this.board = Objects.requireNonNull(board);
    }

    public void applyAreaDamage(int centerX, int centerY, int radius, double damage) {
        // Intentionally small in this step. Entity damage will be moved here when
        // explosive plants are implemented.
    }
}
