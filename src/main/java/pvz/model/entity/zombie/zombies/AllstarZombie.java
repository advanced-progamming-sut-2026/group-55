package pvz.model.entity.zombie.zombies;

import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieSpec;

public class AllstarZombie extends Zombie {

    private boolean charging = true;

    public AllstarZombie(ZombieSpec spec) {
        super(spec);

        // شروع با سرعت دو برابر
        setTilesPerSecond(spec.getSpeed() * 2);
    }


    @Override
    protected void bite(Plant plant) {

        if (charging) {

            // ضربه مرگبار هنگام دویدن
            plant.takeDamage(Double.MAX_VALUE);

            charging = false;

            // بعد از برخورد، نصف سرعت عادی
            setTilesPerSecond(getSpec().getSpeed() / 2);

        } else {

            // خوردن عادی
            super.bite(plant);
        }
    }
}
