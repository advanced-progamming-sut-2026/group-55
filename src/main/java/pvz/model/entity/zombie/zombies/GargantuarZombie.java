package pvz.model.entity.zombie.zombies;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieSpec;
import pvz.model.entity.zombie.ZombieFactory;

public class GargantuarZombie extends Zombie {

    private final ZombieFactory factory;
    private boolean impThrown = false;

    public GargantuarZombie(ZombieSpec spec, ZombieFactory factory) {
        super(spec);
        this.factory = factory;
    }


    @Override
    protected void bite(Plant plant) {
        plant.takeDamage(Double.MAX_VALUE);
    }


    @Override
    public void update(long tick) {
        super.update(tick);

        checkThrowImp();
    }


    private void checkThrowImp() {

        if (impThrown || isDead()) {
            return;
        }

        if (getHealth() < getSpec().getHitpoints() / 2.0) {
            throwImp();
            impThrown = true;
        }
    }


    private void throwImp() {

        Zombie imp = factory.create("imp");

        imp.spawn(
                getWorld(),
                Math.max(0, getTileX() - 2),
                getTileY()
        );
    }
}
