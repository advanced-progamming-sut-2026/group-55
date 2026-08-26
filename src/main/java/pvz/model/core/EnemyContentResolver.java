package pvz.model.core;

import java.util.List;
import java.util.Objects;

import pvz.model.core.board.Board;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.projectile.ProjectileType;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;

/**
 * Applies explosive style damage to every non friendly content of the lawn.
 * Plants, plant armor and any other component owned by the player are never
 * touched here; only zombies, pushed obstacles and destructible tile content
 * are damaged.
 */
final class EnemyContentResolver {

    private final World world;

    EnemyContentResolver(World world) {
        this.world = Objects.requireNonNull(world, "world cannot be null");
    }

    boolean hasEnemyContentAt(int column, int row) {
        Board board = world.board();

        if (!board.inBounds(column, row)) {
            return false;
        }

        boolean zombiePresent = world.getZombies().stream()
                .anyMatch(zombie -> !zombie.isDead()
                        && zombie.getTileX() == column
                        && zombie.getTileY() == row);

        return zombiePresent
                || world.hasPushedObstacleInTile(column, row)
                || board.getTile(column, row).hasDestructibleContent();
    }

    void damageInArea(
            int column,
            int row,
            int radius,
            double damage
    ) {
        validateDamage(damage);

        world.board().damageZombiesInArea(
                world.getZombies(),
                column,
                row,
                radius,
                damage
        );

        world.damagePushedObstaclesDirectlyInArea(
                column,
                row,
                radius,
                damage
        );

        world.board().damageTilesInArea(
                column,
                row,
                radius,
                damage
        );
    }

    void damageInRow(int row, double damage) {
        validateDamage(damage);

        Board board = world.board();

        if (!board.inBounds(1, row)) {
            throw new IndexOutOfBoundsException(
                    "row " + row + " is out of bounds"
            );
        }

        for (Zombie zombie : world.getZombies()) {
            if (zombie.getTileY() == row) {
                zombie.takeAbilityDamage(
                        damage,
                        DamageContext.ImpactMode.AREA
                );
            }
        }

        for (PushedObstacle obstacle : world.getPushedObstacles()) {
            if (obstacle.getTileY() == row) {
                obstacle.takeDirectDamage(damage);
            }
        }

        for (int column = 1; column <= board.getCols(); column++) {
            board.damageAllDestructibleContent(
                    column,
                    row,
                    damage
            );
        }
    }

    void damageEverything(double damage) {
        validateDamage(damage);

        for (Zombie zombie : world.getZombies()) {
            zombie.takeAbilityDamage(
                        damage,
                        DamageContext.ImpactMode.AREA
                );
        }

        for (PushedObstacle obstacle : world.getPushedObstacles()) {
            obstacle.takeDirectDamage(damage);
        }

        for (int row = 1; row <= world.board().getRows(); row++) {
            for (int column = 1; column <= world.board().getCols(); column++) {
                world.board().damageAllDestructibleContent(
                        column,
                        row,
                        damage
                );
            }
        }
    }

    void destroyFireVulnerableObstaclesInRow(int row) {
        for (PushedObstacle obstacle : world.getPushedObstacles()) {
            if (obstacle.getTileY() == row
                    && obstacle.meltsOnFire()
                    && !obstacle.isDead()) {
                obstacle.takeProjectileDamage(
                        ProjectileType.FIRE,
                        obstacle.getHealth()
                );
            }
        }
    }

    void notifyHostilePresentAt(
            int column,
            int row,
            long currentTick
    ) {
        if (!world.board().inBounds(column, row)) {
            return;
        }

        List<Plant> plants = world.board()
                .getTile(column, row)
                .getPlants();

        for (Plant plant : plants) {
            plant.tryTriggerOnHostileContact(currentTick);
        }
    }

    private void validateDamage(double damage) {
        if (!Double.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException(
                    "enemy damage must be finite and non-negative"
            );
        }
    }
}
