package pvz.model.entity.plant.category.wall;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pvz.model.core.World;
import pvz.model.entity.zombie.Zombie;

final class ZombieLaneMover {

    private ZombieLaneMover() {
    }

    static boolean moveToAdjacentRow(
            World world,
            Zombie zombie,
            int row
    ) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(zombie, "zombie cannot be null");

        if (zombie.isDead()) {
            return false;
        }

        List<Integer> candidates = adjacentRows(world, row);

        if (candidates.isEmpty()) {
            return false;
        }

        int targetRow = candidates.size() == 1
                ? candidates.getFirst()
                : candidates.get(world.randomInt(candidates.size()));

        zombie.moveToRow(targetRow);

        return true;
    }

    static boolean moveToRow(Zombie zombie, int row) {
        Objects.requireNonNull(zombie, "zombie cannot be null");

        if (zombie.isDead() || zombie.getRow() == row) {
            return false;
        }

        zombie.moveToRow(row);

        return true;
    }

    private static List<Integer> adjacentRows(World world, int row) {
        List<Integer> rows = new ArrayList<>(2);

        if (row - 1 >= 1) {
            rows.add(row - 1);
        }

        if (row + 1 <= world.board().getRows()) {
            rows.add(row + 1);
        }

        return rows;
    }
}
