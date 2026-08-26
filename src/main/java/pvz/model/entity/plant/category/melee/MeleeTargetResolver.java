package pvz.model.entity.plant.category.melee;

import java.util.Comparator;
import java.util.List;

import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.Zombie;

final class MeleeTargetResolver {

    private MeleeTargetResolver() {
    }

    static Zombie nearestAround(
            Plant owner,
            World world,
            int rangeTiles
    ) {
        return candidatesAround(owner, world, rangeTiles).stream()
                .min(Comparator.comparingDouble(
                        zombie -> Math.abs(zombie.getX() - owner.getX())
                ))
                .orElse(null);
    }

    static Zombie nearestAhead(
            Plant owner,
            World world,
            int rangeTiles
    ) {
        return world.getZombies().stream()
                .filter(zombie -> isValid(zombie, owner))
                .filter(zombie -> zombie.getTileX() >= owner.getTileX())
                .filter(zombie -> zombie.getTileX()
                        <= owner.getTileX() + rangeTiles)
                .min(Comparator.comparingDouble(
                        zombie -> Math.abs(zombie.getX() - owner.getX())
                ))
                .orElse(null);
    }

    static List<Zombie> candidatesAhead(
            Plant owner,
            World world,
            int rangeTiles
    ) {
        return world.getZombies().stream()
                .filter(zombie -> isValid(zombie, owner))
                .filter(zombie -> zombie.getTileX() >= owner.getTileX())
                .filter(zombie -> zombie.getTileX()
                        <= owner.getTileX() + rangeTiles)
                .sorted(Comparator.comparingDouble(
                        zombie -> Math.abs(zombie.getX() - owner.getX())
                ))
                .toList();
    }

    private static List<Zombie> candidatesAround(
            Plant owner,
            World world,
            int rangeTiles
    ) {
        return world.getZombies().stream()
                .filter(zombie -> isValid(zombie, owner))
                .filter(zombie -> Math.abs(
                        zombie.getTileX() - owner.getTileX()
                ) <= rangeTiles)
                .toList();
    }

    private static boolean isValid(Zombie zombie, Plant owner) {
        return !zombie.isDead()
                && zombie.getTileY() == owner.getTileY();
    }
}
