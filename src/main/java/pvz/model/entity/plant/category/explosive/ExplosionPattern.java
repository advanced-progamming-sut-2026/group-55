package pvz.model.entity.plant.category.explosive;

import java.util.Objects;

import pvz.model.core.World;

public enum ExplosionPattern {

    TILE,

    AREA,

    ROW,

    LAWN;

    public void damageEnemyContents(
            World world,
            int column,
            int row,
            int radius,
            double damage
    ) {
        Objects.requireNonNull(world, "world cannot be null");

        switch (this) {
            case TILE -> world.damageEnemyContentsInArea(
                    column,
                    row,
                    0,
                    damage
            );

            case AREA -> world.damageEnemyContentsInArea(
                    column,
                    row,
                    radius,
                    damage
            );

            case ROW -> world.damageEnemyContentsInRow(row, damage);

            case LAWN -> world.damageAllEnemyContents(damage);

            default -> throw new IllegalStateException(
                    "unsupported explosion pattern: " + this
            );
        }
    }
}
