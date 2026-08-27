package pvz.model.entity.plant.category.homing;

import java.util.Objects;

import pvz.model.entity.projectile.homing.HomingImpact;
import pvz.model.entity.zombie.HypnosisService;
import pvz.model.entity.zombie.Zombie;

final class HypnosisImpact implements HomingImpact {

    @Override
    public void hitZombie(Zombie zombie, long currentTick) {
        Objects.requireNonNull(zombie, "zombie cannot be null");

        HypnosisService.hypnotize(zombie, currentTick);
    }
}
