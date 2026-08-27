package pvz.model.entity.plant.category.homing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.projectile.homing.HomingTarget;
import pvz.model.entity.projectile.homing.PushedObstacleHomingTarget;
import pvz.model.entity.projectile.homing.TileHomingTarget;
import pvz.model.entity.projectile.homing.ZombieHomingTarget;
import pvz.model.entity.zombie.PushedObstacle;
import pvz.model.entity.zombie.Zombie;

/**
 * Selects homing targets. Every random pick uses the injected world RNG so
 * the behavior stays deterministic in tests.
 */
public final class HomingTargetResolver {

    private HomingTargetResolver() {
    }

    public static boolean hasHostileZombie(World world) {
        return !world.getHostileZombies().isEmpty();
    }

    public static HomingTarget randomHostileZombie(World world) {
        return randomOf(world, hostileZombieTargets(world));
    }

    public static HomingTarget randomDestructibleTarget(World world) {
        return randomOf(world, destructibleTargets(world));
    }

    public static HomingTarget catTailPriorityZombie(
            World world,
            Plant owner
    ) {
        return byCatTailPriority(hostileZombieTargets(world), owner);
    }

    public static HomingTarget catTailPriorityDestructibleTarget(
            World world,
            Plant owner
    ) {
        return byCatTailPriority(destructibleTargets(world), owner);
    }

    public static List<HomingTarget> randomDistinctHostileZombies(
            World world,
            int count
    ) {
        return randomDistinct(world, hostileZombieTargets(world), count);
    }

    public static List<HomingTarget> randomDistinctDestructibleTargets(
            World world,
            int count
    ) {
        return randomDistinct(world, destructibleTargets(world), count);
    }

    public static List<HomingTarget> hostileZombieTargets(World world) {
        Objects.requireNonNull(world, "world cannot be null");

        return world.getHostileZombies().stream()
                .filter(zombie -> !zombie.isDead())
                .map(zombie -> (HomingTarget) new ZombieHomingTarget(zombie))
                .toList();
    }

    public static List<HomingTarget> destructibleTargets(World world) {
        Objects.requireNonNull(world, "world cannot be null");

        List<HomingTarget> targets = new ArrayList<>();

        for (int column = 1; column <= world.board().getCols(); column++) {
            for (int row = 1; row <= world.board().getRows(); row++) {
                if (world.board().getTile(column, row)
                        .hasDestructibleContent()) {
                    targets.add(new TileHomingTarget(world, column, row));
                }
            }
        }

        for (PushedObstacle obstacle : world.getPushedObstacles()) {
            if (!obstacle.isDead()) {
                targets.add(new PushedObstacleHomingTarget(obstacle));
            }
        }

        return List.copyOf(targets);
    }

    private static HomingTarget byCatTailPriority(
            List<HomingTarget> candidates,
            Plant owner
    ) {
        Objects.requireNonNull(owner, "owner plant cannot be null");

        return candidates.stream()
                .min(catTailPriority(owner))
                .orElse(null);
    }

    private static Comparator<HomingTarget> catTailPriority(Plant owner) {
        return Comparator
                .comparingDouble(HomingTarget::getX)
                .thenComparingDouble(target -> squaredDistance(target, owner))
                .thenComparingDouble(HomingTarget::getY)
                .thenComparingDouble(HomingTarget::getX);
    }

    private static double squaredDistance(HomingTarget target, Plant owner) {
        double deltaX = target.getX() - owner.getX();
        double deltaY = target.getY() - owner.getY();

        return deltaX * deltaX + deltaY * deltaY;
    }

    private static HomingTarget randomOf(
            World world,
            List<HomingTarget> candidates
    ) {
        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.get(world.randomInt(candidates.size()));
    }

    private static List<HomingTarget> randomDistinct(
            World world,
            List<HomingTarget> candidates,
            int count
    ) {
        if (count <= 0) {
            throw new IllegalArgumentException(
                    "target count must be positive"
            );
        }

        List<HomingTarget> pool = new ArrayList<>(candidates);
        List<HomingTarget> selected = new ArrayList<>();

        while (!pool.isEmpty() && selected.size() < count) {
            selected.add(pool.remove(world.randomInt(pool.size())));
        }

        return List.copyOf(selected);
    }

    public static Zombie zombieOf(HomingTarget target) {
        return target instanceof ZombieHomingTarget zombieTarget
                ? zombieTarget.zombie()
                : null;
    }
}
