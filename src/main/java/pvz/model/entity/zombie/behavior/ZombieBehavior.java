package pvz.model.entity.zombie.behavior;

import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.ArmorInstance;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.Zombie;

public interface ZombieBehavior {
    default void onSpawn(Zombie zombie, World world, long currentTick) {}

    default void onTick(Zombie zombie, World world, long currentTick) {}

    default boolean onPlantEncounter(
            Zombie zombie,
            Plant plant,
            World world,
            long currentTick
    ) {
        return false;
    }

    default DamageContext onIncomingHit(
            Zombie zombie,
            DamageContext context
    ) {
        return context;
    }

    default void onArmorBroken(
            Zombie zombie,
            ArmorInstance armor,
            long currentTick
    ) {}

    default void onDeath(Zombie zombie, World world, long currentTick) {}
}
