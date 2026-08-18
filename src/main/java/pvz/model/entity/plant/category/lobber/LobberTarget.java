package pvz.model.entity.plant.category.lobber;

import java.util.Objects;

import pvz.model.entity.zombie.Zombie;

public final class LobberTarget {
    private final Zombie zombie;
    private final int targetRow;
    private int lastKnownColumn;
    private boolean zombieLost;

    private LobberTarget(
            Zombie zombie,
            int column,
            int row
    ) {
        if (column <= 0 || row <= 0) {
            throw new IllegalArgumentException(
                    "lobber target coordinates must be positive"
            );
        }

        this.zombie = zombie;
        this.targetRow = row;
        this.lastKnownColumn = column;
    }

    public static LobberTarget zombie(Zombie zombie) {
        Zombie checkedZombie = Objects.requireNonNull(
                zombie,
                "target zombie cannot be null"
        );

        return new LobberTarget(
                checkedZombie,
                checkedZombie.getTileX(),
                checkedZombie.getTileY()
        );
    }

    public static LobberTarget terrain(
            int column,
            int row
    ) {
        return new LobberTarget(null, column, row);
    }

    public boolean targetsZombie() {
        return zombie != null;
    }

    public Zombie zombie() {
        if (zombie == null) {
            throw new IllegalStateException(
                    "this lobber target is terrain"
            );
        }

        return zombie;
    }

    public int currentColumn() {
        refreshLastKnownPosition();
        return lastKnownColumn;
    }

    public int currentRow() {
        refreshLastKnownPosition();
        return targetRow;
    }

    public boolean hasLostZombie() {
        refreshLastKnownPosition();
        return zombieLost;
    }

    private void refreshLastKnownPosition() {
        if (zombie == null || zombie.isDead() || zombieLost) {
            return;
        }

        if (zombie.getTileY() != targetRow) {
            zombieLost = true;
            return;
        }

        lastKnownColumn = zombie.getTileX();
    }
}
