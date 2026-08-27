package pvz.model.entity.plant.category.melee;

import java.util.ArrayList;
import java.util.List;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.Zombie;

final class ChomperBehavior extends AbstractMeleeBehavior {

    private long digestUntilTick;

    ChomperBehavior(Plant owner, MeleeProfile profile) {
        super(owner, profile);
    }

    @Override
    protected void afterPlaced() {
        super.afterPlaced();
        digestUntilTick = placedTick();
    }

    @Override
    public boolean canStartAction(long currentTick) {
        return currentTick >= digestUntilTick
                && MeleeTargetResolver.nearestAhead(
                        owner(),
                        world(),
                        profile().rangeTiles()
                ) != null;
    }

    @Override
    protected boolean hasAttackTarget(long currentTick) {
        return currentTick >= digestUntilTick
                && MeleeTargetResolver.nearestAhead(
                        owner(),
                        world(),
                        profile().rangeTiles()
                ) != null;
    }

    @Override
    protected void performAttack(long currentTick) {
        Zombie target = MeleeTargetResolver.nearestAhead(
                owner(),
                world(),
                profile().rangeTiles()
        );
        if (target == null) {
            return;
        }
        target.takeDirectDamage(Double.MAX_VALUE);
        if (!target.isDead()) {
            return;
        }

        digestUntilTick = currentTick + profile().digestTicks();
        markAttack(currentTick, MeleeAttackDirection.SWALLOW);
        publishAttack("swallowed a zombie and started digesting.");
    }

    @Override
    protected void scheduleNextAction(long currentTick) {
        // Digestion owns Chomper's cooldown; the generic interval is not used.
    }

    @Override
    public boolean isDigesting(long currentTick) {
        return currentTick < digestUntilTick;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        List<Zombie> targets = new ArrayList<>(
                MeleeTargetResolver.candidatesAhead(
                        owner(),
                        world(),
                        profile().plantFoodRangeTiles()
                )
        );
        int attempts = Math.min(
                profile().plantFoodTargetCount(),
                targets.size()
        );
        int swallowed = 0;
        for (int index = 0; index < attempts; index++) {
            Zombie target = targets.get(index);
            target.takeDirectDamage(Double.MAX_VALUE);
            if (target.isDead()) {
                swallowed++;
            }
        }

        if (swallowed > 0) {
            digestUntilTick = currentTick + profile().digestTicks();
        }
        markAttack(currentTick, MeleeAttackDirection.SWALLOW);
        publishAttack(
                "swallowed " + swallowed + " zombies with plant food."
        );
    }
}
