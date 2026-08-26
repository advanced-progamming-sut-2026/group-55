package pvz.model.entity.plant.category.explosive;

import pvz.model.entity.plant.Plant;

final class GrapeshotBehavior extends AbstractExplosiveBehavior {

    private GrapeshotVolley volley;

    GrapeshotBehavior(Plant owner, ExplosiveProfile profile) {
        super(owner, profile);
    }

    @Override
    protected void afterPlaced() {
        triggerEffect(placedTick());
    }

    @Override
    protected void applyEffect(long currentTick) {
        world().damageEnemyContentsInArea(
                column(),
                row(),
                profile().explosionRadius(),
                profile().damage()
        );

        volley = new GrapeshotVolley(
                world(),
                column(),
                row(),
                profile(),
                currentTick
        );

        world().game().register(volley);

        publishEffect("exploded and launched grapes.");
    }

    GrapeshotVolley volley() {
        return volley;
    }
}
