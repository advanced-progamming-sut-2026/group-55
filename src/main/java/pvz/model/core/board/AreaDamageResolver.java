package pvz.model.core.board;

import java.util.List;
import java.util.Objects;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.hit.PlantHitSource;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.Zombie;

final class AreaDamageResolver {

    private final BoardGrid grid;

    AreaDamageResolver(BoardGrid grid) {
        this.grid = Objects.requireNonNull(
                grid,
                "grid cannot be null"
        );
    }

    void damageZombiesDirectly(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            double damage
    ) {
        applyToZombies(
                zombies,
                centerX,
                centerY,
                radius,
                damage,
                zombie -> zombie.takeDirectDamage(damage)
        );
    }

    void damageZombiesWithProjectile(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            double damage,
            ProjectileType projectileType,
            DamageContext.AttackDelivery delivery,
            long currentTick
    ) {
        Objects.requireNonNull(
                projectileType,
                "projectile type cannot be null"
        );
        Objects.requireNonNull(delivery, "attack delivery cannot be null");

        if (currentTick < 0) {
            throw new IllegalArgumentException(
                    "current tick cannot be negative"
            );
        }

        applyToZombies(
                zombies,
                centerX,
                centerY,
                radius,
                damage,
                zombie -> projectileType.hitZombie(
                        zombie,
                        damage,
                        currentTick,
                        delivery,
                        DamageContext.ImpactMode.AREA
                )
        );
    }

    void damageTilesWithProjectile(
            int centerX,
            int centerY,
            int radius,
            double baseDamage,
            ProjectileType projectileType
    ) {
        Objects.requireNonNull(
                projectileType,
                "projectile type cannot be null"
        );
        validateArea(centerX, centerY, radius, baseDamage);

        for (int x = minimumX(centerX, radius);
             x <= maximumX(centerX, radius);
             x++) {
            for (int y = minimumY(centerY, radius);
                 y <= maximumY(centerY, radius);
                 y++) {
                ElementInteractionResolver.damageTile(
                        grid.getTile(x, y),
                        projectileType,
                        baseDamage
                );
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

    private void applyToZombies(
            List<Zombie> zombies,
            int centerX,
            int centerY,
            int radius,
            double damage,
            ZombieDamageOperation operation
    ) {
        Objects.requireNonNull(
                zombies,
                "zombies cannot be null"
        );
        Objects.requireNonNull(
                operation,
                "zombie damage operation cannot be null"
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
                operation.apply(zombie);
            }
        }
    }

    private void damagePlantsInTile(
            int x,
            int y,
            double damage
    ) {
        List<Plant> plants = grid.getTile(x, y).getPlants();
        if (plants.isEmpty()) {
            return;
        }

        plants.getLast().receiveHit(
                PlantHitSource.AREA_DAMAGE,
                null,
                damage
        );
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

        if (!Double.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException(
                    "area damage must be finite and non-negative"
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

    @FunctionalInterface
    private interface ZombieDamageOperation {
        void apply(Zombie zombie);
    }
}
