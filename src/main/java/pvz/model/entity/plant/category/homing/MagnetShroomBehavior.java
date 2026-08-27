package pvz.model.entity.plant.category.homing;

import java.util.Comparator;
import java.util.Set;

import pvz.model.core.GameEvents;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.ArmorInstance;
import pvz.model.entity.zombie.ArmorSpec;
import pvz.model.entity.zombie.Zombie;

final class MagnetShroomBehavior extends AbstractHomingBehavior {

    private static final Set<String> MAGNETIZABLE_ARMOR_IDS = Set.of(
            "BUCKET",
            "CROWN"
    );

    MagnetShroomBehavior(Plant owner, HomingProfile profile) {
        super(owner, profile);
    }

    @Override
    protected boolean hasTarget(long currentTick) {
        return nearestMagnetizableZombie() != null;
    }

    @Override
    protected boolean fireOnce(long currentTick) {
        Zombie target = nearestMagnetizableZombie();

        if (target == null) {
            return false;
        }

        return detachOneArmor(target);
    }

    @Override
    public void applyPlantFood(long currentTick, long durationTicks) {
        ensurePlaced();

        for (Zombie zombie : world().getHostileZombies()) {
            if (!zombie.isDead()) {
                detachOneArmor(zombie);
            }
        }
    }

    private Zombie nearestMagnetizableZombie() {
        return world().getHostileZombies().stream()
                .filter(zombie -> !zombie.isDead())
                .filter(this::hasMagnetizableArmor)
                .min(Comparator
                        .comparingDouble(this::squaredDistanceToOwner)
                        .thenComparingDouble(Zombie::getX)
                        .thenComparingDouble(Zombie::getY))
                .orElse(null);
    }

    private boolean hasMagnetizableArmor(Zombie zombie) {
        return zombie.getArmorSet().layers().stream()
                .anyMatch(layer -> !layer.isBroken()
                        && isMagnetizable(layer.spec()));
    }

    private boolean detachOneArmor(Zombie zombie) {
        ArmorInstance detached = zombie.getArmorSet()
                .detachFirstIntactArmor(this::isMagnetizable);

        if (detached == null) {
            return false;
        }

        GameEvents.publish(
                owner().getName()
                        + " pulled "
                        + detached.spec().name()
                        + " from "
                        + zombie.getName()
                        + "."
        );

        return true;
    }

    private boolean isMagnetizable(ArmorSpec armor) {
        return MAGNETIZABLE_ARMOR_IDS.stream()
                .anyMatch(id -> id.equalsIgnoreCase(armor.id()));
    }

    private double squaredDistanceToOwner(Zombie zombie) {
        double deltaX = zombie.getX() - owner().getX();
        double deltaY = zombie.getY() - owner().getY();

        return deltaX * deltaX + deltaY * deltaY;
    }
}
