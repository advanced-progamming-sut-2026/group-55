package pvz.model.entity.plant.category.explosive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.capability.ArmableTrapCapability;
import pvz.model.entity.plant.behavior.capability.ContactTriggerCapability;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;

final class ContactMineBehavior extends AbstractExplosiveBehavior
        implements ContactTriggerCapability,
        ArmableTrapCapability,
        PlantFoodCapability {

    private long armTick = Long.MAX_VALUE;

    private boolean armedImmediately;

    ContactMineBehavior(Plant owner, ExplosiveProfile profile) {
        super(owner, profile);
    }

    @Override
    protected void afterPlaced() {
        armTick = placedTick() + profile().armDelayTicks();
    }

    @Override
    protected boolean edibleWhileIdle() {
        return false;
    }

    @Override
    public boolean isArmed(long currentTick) {
        return armedImmediately || currentTick >= armTick;
    }

    @Override
    public void armImmediately(long currentTick) {
        ensurePlaced();
        armedImmediately = true;
        tryTriggerOnHostileContact(currentTick);
    }

    @Override
    public boolean tryTriggerOnHostileContact(long currentTick) {
        if (!canTrigger() || !isArmed(currentTick)) {
            return false;
        }

        if (!world().hasEnemyContentAt(column(), row())) {
            return false;
        }

        return triggerEffect(currentTick);
    }

    @Override
    protected void onIdleTick(long currentTick) {
        tryTriggerOnHostileContact(currentTick);
    }

    @Override
    protected boolean resolvesEffectImmediately() {
        return true;
    }

    @Override
    protected void applyEffect(long currentTick) {
        world().damageEnemyContentsInArea(
                column(),
                row(),
                profile().explosionRadius(),
                profile().damage()
        );

        publishEffect("exploded.");
    }

    @Override
    public boolean supportsPlantFood() {
        return true;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        ensurePlaced();

        armImmediately(currentTick);

        plantArmedClones(currentTick);
    }

    private void plantArmedClones(long currentTick) {
        int columns = world().board().getCols();
        int rows = world().board().getRows();
        int requestedClones = profile().plantFoodCloneCount();

        List<int[]> candidates = new ArrayList<>(columns * rows);

        for (int column = 1; column <= columns; column++) {
            for (int row = 1; row <= rows; row++) {
                candidates.add(new int[]{column, row});
            }
        }

        for (int index = candidates.size() - 1; index > 0; index--) {
            int swapIndex = world().randomInt(index + 1);
            Collections.swap(candidates, index, swapIndex);
        }

        int created = 0;

        for (int[] candidate : candidates) {
            if (created >= requestedClones) {
                break;
            }

            if (plantClone(candidate[0], candidate[1], currentTick)) {
                created++;
            }
        }
    }

    private boolean plantClone(int column, int row, long currentTick) {
        Plant clone = new Plant(owner().getSpec());

        world().board().plant(column, row, clone);

        if (!world().board().getTile(column, row)
                .getPlants().contains(clone)) {
            return false;
        }

        clone.place(world(), column, row, currentTick);
        world().game().register(clone);

        ArmableTrapCapability trap = clone.behaviorCapability(
                ArmableTrapCapability.class
        );

        if (trap != null) {
            trap.armImmediately(currentTick);
        }

        return true;
    }
}
