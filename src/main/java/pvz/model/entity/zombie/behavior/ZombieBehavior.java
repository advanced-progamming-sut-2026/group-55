package pvz.model.entity.zombie.behavior;

import pvz.model.core.World;
import pvz.model.entity.plant.Plant;
import pvz.model.entity.zombie.ArmorInstance;
import pvz.model.entity.zombie.DamageContext;
import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieAllegiance;

public interface ZombieBehavior {
    default void onSpawn(Zombie zombie, World world, long currentTick) {}

    default void onTick(Zombie zombie, World world, long currentTick) {}

    default void onHardStopTick(
            Zombie zombie,
            World world,
            long currentTick
    ) {}

    default void onPositionChanged(
            Zombie zombie,
            World world,
            long currentTick
    ) {}

    default void onAllegianceChanged(
            Zombie zombie,
            World world,
            ZombieAllegiance oldAllegiance,
            ZombieAllegiance newAllegiance,
            long currentTick
    ) {}

    default boolean onPlantEncounter(
            Zombie zombie,
            Plant plant,
            World world,
            long currentTick
    ) {
        return false;
    }

    /**
     * Gives special collision-oriented zombie behaviors a chance to handle an
     * opposing-allegiance zombie before generic zombie-vs-zombie combat starts.
     * Returning {@code true} means the encounter was fully handled for this
     * tick. Behaviors that do not have special contact semantics should keep
     * the default and fall through to the shared combat controller.
     */
    default boolean onOpposingZombieEncounter(
            Zombie zombie,
            Zombie opponent,
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

    default void onAcceptedHit(
            Zombie zombie,
            DamageContext context
    ) {}

    default void onArmorBroken(
            Zombie zombie,
            ArmorInstance armor,
            long currentTick
    ) {}

    default void onDeath(Zombie zombie, World world, long currentTick) {}

    default double modifyMovementMultiplier(
            Zombie zombie,
            World world,
            long currentTick,
            double multiplier
    ) {
        return multiplier;
    }

    default double modifyBiteDamage(
            Zombie zombie,
            Plant plant,
            long currentTick,
            double damage
    ) {
        return damage;
    }

    default boolean convertsFreezeToChill() {
        return false;
    }
}
