package pvz.model.entity.plant.category.lobber;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import pvz.model.core.World;
import pvz.model.entity.zombie.Zombie;

final class LobberTargetResolver {
    private final World world;
    private final int sourceColumn;
    private final int sourceRow;

    LobberTargetResolver(
            World world,
            int sourceColumn,
            int sourceRow
    ) {
        this.world = Objects.requireNonNull(
                world,
                "world cannot be null"
        );

        if (!world.board().inBounds(sourceColumn, sourceRow)) {
            throw new IllegalArgumentException(
                    "lobber source location is out of bounds"
            );
        }

        this.sourceColumn = sourceColumn;
        this.sourceRow = sourceRow;
    }

    LobberTarget findTarget() {
        Zombie zombie = findNearestZombieAhead();

        if (zombie != null) {
            return LobberTarget.zombie(zombie);
        }

        return findNearestTerrainAhead();
    }

    List<LobberTarget> findPlantFoodTargets() {
        List<LobberTarget> targets = new ArrayList<>();

        world.getZombies().stream()
                .filter(zombie -> !zombie.isDead())
                .map(LobberTarget::zombie)
                .forEach(targets::add);

        for (int row = 1; row <= world.board().getRows(); row++) {
            for (int column = 1;
                 column <= world.board().getCols();
                 column++) {
                if (world.board()
                        .getTile(column, row)
                        .hasDestructibleContent()) {
                    targets.add(
                            LobberTarget.terrain(column, row)
                    );
                }
            }
        }

        return List.copyOf(targets);
    }

    private Zombie findNearestZombieAhead() {
        double sourceX = sourceColumn - 0.5;

        return world.getZombies().stream()
                .filter(zombie -> !zombie.isDead())
                .filter(zombie -> zombie.getTileY() == sourceRow)
                .filter(zombie -> zombie.getX() >= sourceX)
                .min(Comparator.comparingDouble(Zombie::getX))
                .orElse(null);
    }

    private LobberTarget findNearestTerrainAhead() {
        for (int column = sourceColumn + 1;
             column <= world.board().getCols();
             column++) {
            if (world.board()
                    .getTile(column, sourceRow)
                    .hasDestructibleContent()) {
                return LobberTarget.terrain(
                        column,
                        sourceRow
                );
            }
        }

        return null;
    }
}
