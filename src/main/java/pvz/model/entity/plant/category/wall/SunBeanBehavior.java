package pvz.model.entity.plant.category.wall;

import pvz.model.core.GameEvents;
import pvz.model.entity.collectible.sun.SunValue;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.behavior.capability.PlantHitReactionCapability;
import pvz.model.entity.plant.behavior.capability.SunProductionCapability;
import pvz.model.entity.plant.category.sun.ProducedSunSpawner;
import pvz.model.entity.plant.hit.PlantHitContext;

final class SunBeanBehavior extends ArmoredWallBehavior
        implements PlantHitReactionCapability,
        SunProductionCapability {

    /**
     * Cumulative damage that must actually reach Sun Bean's armor or
     * health before one standard 100-value sun is produced. Partial
     * progress is kept across hits, while blocked and overkill damage
     * never contributes.
     */
    public static final double DAMAGE_PER_SUN_DROP = 250;

    public static final int SUN_DROP_VALUE =
            SunValue.SPECIALSUN.getValue();

    private double unconvertedDamage;

    private int pendingSuns;

    SunBeanBehavior(
            Plant owner,
            boolean blocksVaulting,
            double armorCapacity
    ) {
        super(owner, blocksVaulting, armorCapacity);
    }

    @Override
    public void onPlantHit(PlantHitContext context) {
        double appliedDamage = context.armorAbsorbedDamage()
                + context.healthDamage();
        int drops = convertDamageToDrops(appliedDamage);

        if (drops <= 0) {
            return;
        }

        for (int drop = 0; drop < drops; drop++) {
            spawnSunDrop();
        }

        GameEvents.publish(
                "plant "
                        + owner().getName()
                        + " produced "
                        + drops
                        + " sun(value: "
                        + SUN_DROP_VALUE
                        + ") at ("
                        + column()
                        + ", "
                        + row()
                        + ")"
        );
    }

    @Override
    public boolean hasPendingSuns() {
        return pendingSuns > 0;
    }

    @Override
    public void onProducedSunRemoved() {
        if (pendingSuns > 0) {
            pendingSuns--;
        }
    }

    double getUnconvertedDamage() {
        return unconvertedDamage;
    }

    private int convertDamageToDrops(double damage) {
        if (!(damage > 0)) {
            return 0;
        }

        unconvertedDamage += damage;

        int drops = (int) Math.floor(
                unconvertedDamage / DAMAGE_PER_SUN_DROP
        );

        if (drops <= 0) {
            return 0;
        }

        unconvertedDamage -= drops * DAMAGE_PER_SUN_DROP;

        return drops;
    }

    private void spawnSunDrop() {
        ProducedSunSpawner.spawnAtProducer(
                world(),
                owner(),
                SUN_DROP_VALUE
        );

        pendingSuns++;
    }
}
