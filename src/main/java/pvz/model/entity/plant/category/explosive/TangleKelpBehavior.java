package pvz.model.entity.plant.category.explosive;

import java.util.ArrayList;
import java.util.List;

import pvz.model.core.board.TileType;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.capability.ContactTriggerCapability;
import pvz.model.entity.plant.behavior.capability.PlantFoodCapability;
import pvz.model.entity.zombie.Zombie;

final class TangleKelpBehavior extends AbstractExplosiveBehavior
        implements ContactTriggerCapability, PlantFoodCapability {

    TangleKelpBehavior(Plant owner, ExplosiveProfile profile) {
        super(owner, profile);
    }

    @Override
    public boolean tryTriggerOnHostileContact(long currentTick) {
        if (!canTrigger() || zombieInTile() == null) {
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
        List<Zombie> targets = zombiesInOwnTile();
        int targetCount = Math.min(profile().normalTargetCount(), targets.size());
        for (int index = 0; index < targetCount; index++) {
            dragUnderwater(targets.get(index));
        }

        publishEffect("dragged " + targetCount + " zombie(s) underwater.");
    }

    @Override
    public boolean supportsPlantFood() {
        return true;
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        ensurePlaced();

        List<Zombie> candidates = new ArrayList<>(zombiesInWater());

        int targets = Math.min(
                profile().plantFoodTargetCount(),
                candidates.size()
        );

        for (int index = 0; index < targets; index++) {
            Zombie target = candidates.remove(
                    world().randomInt(candidates.size())
            );

            dragUnderwater(target);
        }
    }

    private List<Zombie> zombiesInWater() {
        return world().getHostileZombies().stream()
                .filter(zombie -> !zombie.isDead())
                .filter(this::standsInWater)
                .toList();
    }

    private boolean standsInWater(Zombie zombie) {
        if (!world().board().inBounds(
                zombie.getTileX(),
                zombie.getTileY()
        )) {
            return false;
        }

        return world().board().getTile(
                zombie.getTileX(),
                zombie.getTileY()
        ).getType() == TileType.WATER;
    }


    private List<Zombie> zombiesInOwnTile() {
        return world().getHostileZombies().stream()
                .filter(zombie -> !zombie.isDead())
                .filter(zombie -> zombie.getTileX() == column()
                        && zombie.getTileY() == row())
                .toList();
    }

    private Zombie zombieInTile() {
        return world().getHostileZombies().stream()
                .filter(zombie -> !zombie.isDead())
                .filter(zombie -> zombie.getTileX() == column()
                        && zombie.getTileY() == row())
                .findFirst()
                .orElse(null);
    }

    private void dragUnderwater(Zombie zombie) {
        zombie.takeDirectDamage(Double.MAX_VALUE);
    }
}
