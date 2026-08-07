package pvz.model.entity.zombie.zombies;

import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieSpec;

public class NewspaperZombie extends Zombie {

    private static final double ENRAGED_SPEED_MULTIPLIER = 2.0;
    private static final double ENRAGED_DAMAGE_MULTIPLIER = 2.0;

    private boolean enraged = false;

    private final double normalSpeed;
    private final double normalDamage;


    public NewspaperZombie(ZombieSpec spec) {
        super(spec);

        this.normalSpeed = spec.getSpeed();
        this.normalDamage = spec.getEatDps();
    }


    @Override
    public void update(long tick) {

        checkNewspaper();

        super.update(tick);
    }


    private void checkNewspaper() {

        if (enraged) {
            return;
        }

        if (getArmorHealth() <= 0) {

            enraged = true;

            setTilesPerSecond(
                    normalSpeed * ENRAGED_SPEED_MULTIPLIER
            );

            setDamagePerSecond(
                    normalDamage * ENRAGED_DAMAGE_MULTIPLIER
            );
        }
    }
}
