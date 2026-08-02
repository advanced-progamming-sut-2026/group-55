package pvz.model.core.board;

import java.util.Objects;
import java.util.Set;
import pvz.model.entity.zombie.Zombie;

/** Handles collision queries for projectiles on a board. */
public final class ProjectileResolver {
    private final Board board;

    public ProjectileResolver(Board board) {
        this.board = Objects.requireNonNull(board);
    }

    public Integer findHitBlockingTile(int row, double fromX, double toX) {
        if (toX > fromX) {
            for (int column = Math.max(1, board.xToColumn(fromX));
                 column <= Math.min(board.getCols(), board.xToColumn(toX));
                 column++) {
                if (board.getTile(column, row).blocksStraightProjectiles()) {
                    return column;
                }
            }
        } else if (toX < fromX) {
            for (int column = Math.min(board.getCols(), board.xToColumnMovingLeft(fromX));
                 column >= Math.max(1, board.xToColumnMovingLeft(toX));
                 column--) {
                if (board.getTile(column, row).blocksStraightProjectiles()) {
                    return column;
                }
            }
        }
        return null;
    }

    public Zombie findHitZombie(int row, double fromX, double toX, Set<Zombie> ignored) {
        Zombie result = null;
        for (Zombie zombie : board.getZombies()) {
            if (ignored.contains(zombie) || zombie.getTileY() != row) continue;
            if (toX > fromX && zombie.getX() > fromX && zombie.getX() <= toX
                    && (result == null || zombie.getX() < result.getX())) {
                result = zombie;
            }
            if (toX < fromX && zombie.getX() < fromX && zombie.getX() >= toX
                    && (result == null || zombie.getX() > result.getX())) {
                result = zombie;
            }
        }
        return result;
    }
}
