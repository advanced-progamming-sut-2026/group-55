package pvz.model.entity.plant.category.wall;

import pvz.model.core.GameEvents;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.plant.lifecycle.PlantThreat;

final class ExplodeONutBehavior extends ArmoredWallBehavior {
    private static final int EXPLOSION_RADIUS = 1;

    private final double explosionDamage;

    ExplodeONutBehavior(
            Plant owner,
            boolean blocksVaulting,
            double armorCapacity,
            double explosionDamage
    ) {
        super(owner, blocksVaulting, armorCapacity);

        if (explosionDamage <= 0) {
            throw new IllegalArgumentException(
                    "explosion damage must be positive"
            );
        }

        this.explosionDamage = explosionDamage;
    }

    @Override
    public void onRemoved(PlantThreat threat) {
        if (!shouldExplode(threat)) {
            return;
        }

        destroyArmor();
        explode(false);
    }

    @Override
    protected void onArmorDestroyed() {
        explode(true);
    }

    private void explode(boolean armorExplosion) {
        world().board().damageZombiesDirectlyInArea(
                world().getZombies(),
                column(),
                row(),
                EXPLOSION_RADIUS,
                explosionDamage
        );

        world().board().damageTilesInArea(
                column(),
                row(),
                EXPLOSION_RADIUS,
                explosionDamage
        );

        GameEvents.publish(
                owner().getName()
                        + " at ("
                        + column()
                        + ", "
                        + row()
                        + ") "
                        + (armorExplosion
                        ? "armor exploded."
                        : "exploded.")
        );
    }

    private boolean shouldExplode(PlantThreat threat) {
        return threat == PlantThreat.DAMAGE
                || threat == PlantThreat.INSTANT_DESTROY;
    }
}
