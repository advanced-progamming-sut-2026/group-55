package pvz.model.core;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import pvz.model.entity.zombie.Zombie;
import pvz.model.entity.zombie.ZombieAllegiance;

/**
 * Canonical registry for every zombie in a world.
 *
 * <p>The master set owns identity/lifecycle membership. Allegiance buckets are
 * indexes over the same zombie objects; changing sides never copies or replaces
 * a zombie, so health, armor, statuses, position and behavior state stay intact.</p>
 */
final class ZombieRegistry {

    private final Set<Zombie> all = new LinkedHashSet<>();
    private final Map<ZombieAllegiance, LinkedHashSet<Zombie>> byAllegiance =
            new EnumMap<>(ZombieAllegiance.class);

    ZombieRegistry() {
        for (ZombieAllegiance allegiance : ZombieAllegiance.values()) {
            byAllegiance.put(allegiance, new LinkedHashSet<>());
        }
    }

    void add(Zombie zombie) {
        Zombie checkedZombie = Objects.requireNonNull(
                zombie,
                "zombie cannot be null"
        );

        if (!all.add(checkedZombie)) {
            throw new IllegalStateException(
                    "zombie is already registered in this world"
            );
        }

        bucket(checkedZombie.getAllegiance()).add(checkedZombie);
    }

    void remove(Zombie zombie) {
        if (zombie == null || !all.remove(zombie)) {
            return;
        }
        bucket(zombie.getAllegiance()).remove(zombie);
    }

    boolean contains(Zombie zombie) {
        return all.contains(zombie);
    }

    void changeAllegiance(
            Zombie zombie,
            ZombieAllegiance newAllegiance
    ) {
        Zombie checkedZombie = Objects.requireNonNull(
                zombie,
                "zombie cannot be null"
        );
        ZombieAllegiance checkedAllegiance = Objects.requireNonNull(
                newAllegiance,
                "zombie allegiance cannot be null"
        );

        if (!all.contains(checkedZombie)) {
            throw new IllegalArgumentException(
                    "zombie is not registered in this world"
            );
        }

        ZombieAllegiance oldAllegiance = checkedZombie.getAllegiance();
        if (oldAllegiance == checkedAllegiance) {
            return;
        }

        bucket(oldAllegiance).remove(checkedZombie);
        checkedZombie.applyAllegianceFromWorld(checkedAllegiance);
        bucket(checkedAllegiance).add(checkedZombie);
    }

    List<Zombie> snapshot() {
        return List.copyOf(all);
    }

    List<Zombie> hostileView() {
        return List.copyOf(bucket(ZombieAllegiance.HOSTILE));
    }

    List<Zombie> alliedView() {
        return List.copyOf(bucket(ZombieAllegiance.ALLIED));
    }

    boolean hasZombieAhead(
            int row,
            double fromX,
            Set<Zombie> ignoredZombies
    ) {
        Objects.requireNonNull(
                ignoredZombies,
                "ignored zombies cannot be null"
        );

        return bucket(ZombieAllegiance.HOSTILE).stream()
                .anyMatch(zombie ->
                        !ignoredZombies.contains(zombie)
                                && zombie.getTileY() == row
                                && zombie.getX() >= fromX
                );
    }

    Zombie findZombieInTile(int column, int row) {
        // This is a physical-occupancy query, not a player-targeting query.
        // Both hostile and allied zombies occupy board space.
        for (Zombie zombie : all) {
            if (!zombie.isDead()
                    && zombie.getTileX() == column
                    && zombie.getTileY() == row) {
                return zombie;
            }
        }

        return null;
    }

    Zombie findOpposingZombieInTile(Zombie self) {
        Objects.requireNonNull(self, "zombie cannot be null");
        return findOpposingZombieInTile(
                self.getAllegiance(),
                self.getTileX(),
                self.getTileY(),
                self
        );
    }

    Zombie findOpposingZombieInTile(
            ZombieAllegiance selfAllegiance,
            int column,
            int row
    ) {
        return findOpposingZombieInTile(
                selfAllegiance,
                column,
                row,
                null
        );
    }

    private Zombie findOpposingZombieInTile(
            ZombieAllegiance selfAllegiance,
            int column,
            int row,
            Zombie excluded
    ) {
        Objects.requireNonNull(
                selfAllegiance,
                "zombie allegiance cannot be null"
        );
        ZombieAllegiance opposing = selfAllegiance == ZombieAllegiance.HOSTILE
                ? ZombieAllegiance.ALLIED
                : ZombieAllegiance.HOSTILE;

        for (Zombie zombie : bucket(opposing)) {
            if (zombie != excluded
                    && !zombie.isDead()
                    && zombie.getTileX() == column
                    && zombie.getTileY() == row) {
                return zombie;
            }
        }
        return null;
    }

    private LinkedHashSet<Zombie> bucket(ZombieAllegiance allegiance) {
        return byAllegiance.get(allegiance);
    }
}
