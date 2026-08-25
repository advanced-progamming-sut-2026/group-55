package pvz.model.entity.zombie.behavior;

import pvz.model.core.GameEvents;
import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.ArmorInstance;
import pvz.model.entity.zombie.Zombie;

public final class NewspaperEnrageBehavior implements ZombieBehavior {
    private final double speedMultiplier;
    private final double damageMultiplier;
    private final String triggerArmorId;
    private boolean enraged;

    public NewspaperEnrageBehavior(
            double speedMultiplier,
            double damageMultiplier,
            String triggerArmorId
    ) {
        this.speedMultiplier = speedMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.triggerArmorId = triggerArmorId;
    }

    @Override
    public void onArmorBroken(
            Zombie zombie,
            ArmorInstance armor,
            long currentTick
    ) {
        if (armor.spec().id().equalsIgnoreCase(triggerArmorId)) {
            enraged = true;
            GameEvents.publish("Newspaper Zombie became enraged.");
        }
    }

    @Override
    public double modifyMovementMultiplier(
            Zombie zombie,
            World world,
            long currentTick,
            double multiplier
    ) {
        return enraged ? multiplier * speedMultiplier : multiplier;
    }

    @Override
    public double modifyBiteDamage(
            Zombie zombie,
            Plant plant,
            long currentTick,
            double damage
    ) {
        return enraged ? damage * damageMultiplier : damage;
    }
}
