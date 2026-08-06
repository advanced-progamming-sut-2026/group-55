package pvz.model.core.board;

import java.util.List;
import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.Zombie;

final class AreaDamageResolver {

    private final BoardGrid grid;

    AreaDamageResolver(BoardGrid grid) {
        this.grid = Objects.requireNonNull(
                grid,
                "grid cannot be null"
        );
    }

    void damageZombies(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            double damage
    ) {
        Objects.requireNonNull(
                zombies,
                "zombies cannot be null"
        );
        validateArea(centerX, centerY, radius, damage);

        for (Zombie zombie : zombies) {
            if (isInsideSquare(
                    zombie.getTileX(),
                    zombie.getTileY(),
                    centerX,
                    centerY,
                    radius
            )) {
                zombie.takeDirectDamage(damage);
            }
        }
    }

    void damagePlants(
            int centerX,
            int centerY,
            int radius,
            double damage
    ) {
        validateArea(centerX, centerY, radius, damage);

        for (int x = minimumX(centerX, radius);
             x <= maximumX(centerX, radius);
             x++) {
            for (int y = minimumY(centerY, radius);
                 y <= maximumY(centerY, radius);
                 y++) {
                damagePlantsInTile(x, y, damage);
            }
        }
    }

    void damageTiles(
            int centerX,
            int centerY,
            int radius,
            double damage
    ) {
        validateArea(centerX, centerY, radius, damage);

        for (int x = minimumX(centerX, radius);
             x <= maximumX(centerX, radius);
             x++) {
            for (int y = minimumY(centerY, radius);
                 y <= maximumY(centerY, radius);
                 y++) {
                grid.getTile(x, y).takeDamage(damage);
            }
        }
    }

    private void damagePlantsInTile(
            int x,
            int y,
            double damage
    ) {
        for (Plant plant : grid.getTile(x, y).getPlants()) {
            plant.takeDamage(damage);
        }
    }

    private void validateArea(
            int centerX,
            int centerY,
            int radius,
            double damage
    ) {
        grid.requireInBounds(centerX, centerY);

        if (radius < 0) {
            throw new IllegalArgumentException(
                    "area radius cannot be negative"
            );
        }

        if (damage < 0) {
            throw new IllegalArgumentException(
                    "area damage cannot be negative"
            );
        }
    }

    private int minimumX(int centerX, int radius) {
        return Math.max(1, centerX - radius);
    }

    private int maximumX(int centerX, int radius) {
        return Math.min(grid.columns(), centerX + radius);
    }

    private int minimumY(int centerY, int radius) {
        return Math.max(1, centerY - radius);
    }

    private int maximumY(int centerY, int radius) {
        return Math.min(grid.rows(), centerY + radius);
    }

    private boolean isInsideSquare(
            int x,
            int y,
            int centerX,
            int centerY,
            int radius
    ) {
        return Math.abs(x - centerX) <= radius
                && Math.abs(y - centerY) <= radius;
    }
}
