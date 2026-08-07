package pvz.model.entity.plant.lobber;

import java.util.Objects;

import pvz.model.entity.zombie.Zombie;

public final class LobberTarget {
    private final Zombie zombie;
    private int lastKnownColumn;
    private int lastKnownRow;

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
        this.lastKnownColumn = column;
        this.lastKnownRow = row;
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
        return lastKnownRow;
    }

    private void refreshLastKnownPosition() {
        if (zombie == null || zombie.isDead()) {
            return;
        }

        lastKnownColumn = zombie.getTileX();
        lastKnownRow = zombie.getTileY();
    }
}
