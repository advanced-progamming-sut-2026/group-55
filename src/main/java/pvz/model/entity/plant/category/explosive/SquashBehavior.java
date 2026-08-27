package pvz.model.entity.plant.category.explosive;

import java.util.ArrayList;
import java.util.List;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.Zombie;

final class SquashBehavior extends AbstractExplosiveBehavior
        implements PlantFoodCapability, SquashTargeting {

    private SquashDirection lastDirection;

    private int lastTargetColumn;

    private int lastTargetRow;

    SquashBehavior(Plant owner, ExplosiveProfile profile) {
        super(owner, profile);
    }

    @Override
    protected void onIdleTick(long currentTick) {
        SquashDirection direction = findTargetDirection();

        if (direction == null) {
            return;
        }

        lastDirection = direction;
        lastTargetColumn = targetColumn(direction);
        lastTargetRow = row();

        triggerEffect(currentTick);
    }

    @Override
    protected void applyEffect(long currentTick) {
        world().damageEnemyContentsInArea(
                lastTargetColumn,
                lastTargetRow,
                0,
                profile().damage()
        );

        publishEffect("crushed tile (" + lastTargetColumn
                + ", " + lastTargetRow + ") toward " + lastDirection + ".");
    }

    @Override
    public SquashDirection getLastDirection() {
        return lastDirection;
    }

    @Override
    public int getLastTargetColumn() {
        return lastTargetColumn;
    }

    @Override
    public int getLastTargetRow() {
        return lastTargetRow;
    }

    @Override
    public boolean supportsPlantFood() {
        return true;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        ensurePlaced();

        List<Zombie> candidates = new ArrayList<>(
                world().getHostileZombies().stream()
                        .filter(zombie -> !zombie.isDead())
                        .toList()
        );

        int targets = Math.min(
                profile().plantFoodTargetCount(),
                candidates.size()
        );

        for (int index = 0; index < targets; index++) {
            Zombie target = candidates.remove(
                    world().randomInt(candidates.size())
            );

            target.takeAbilityDamage(
                    profile().damage(),
                    DamageContext.ImpactMode.SINGLE_TARGET
            );
        }
    }

    private SquashDirection findTargetDirection() {
        int scanDistance = profile().scanDistanceTiles();

        if (hasEnemyAt(column())) {
            return SquashDirection.CENTER;
        }

        if (hasEnemyAt(column() + scanDistance)) {
            return SquashDirection.FORWARD;
        }

        if (hasEnemyAt(column() - scanDistance)) {
            return SquashDirection.BACKWARD;
        }

        return null;
    }

    private boolean hasEnemyAt(int column) {
        return world().board().inBounds(column, row())
                && world().hasEnemyContentAt(column, row());
    }

    private int targetColumn(SquashDirection direction) {
        int scanDistance = profile().scanDistanceTiles();

        return switch (direction) {
            case CENTER -> column();
            case FORWARD -> column() + scanDistance;
            case BACKWARD -> column() - scanDistance;
        };
    }
}
