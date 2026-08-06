package pvz.model.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pvz.model.entity.zombie.Zombie;

final class ZombieRegistry {

    private final List<Zombie> zombies = new ArrayList<>();
    private final List<Zombie> readOnlyView =
            Collections.unmodifiableList(zombies);

    void add(Zombie zombie) {
        Zombie checkedZombie = Objects.requireNonNull(
                zombie,
                "zombie cannot be null"
        );

        if (zombies.contains(checkedZombie)) {
            throw new IllegalStateException(
                    "zombie is already registered in this world"
            );
        }

        zombies.add(checkedZombie);
    }

    void remove(Zombie zombie) {
        zombies.remove(zombie);
    }

    List<Zombie> snapshot() {
        return List.copyOf(zombies);
    }

    List<Zombie> view() {
        return readOnlyView;
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

        return zombies.stream()
                .anyMatch(zombie ->
                        !ignoredZombies.contains(zombie)
                                && zombie.getTileY() == row
                                && zombie.getX() >= fromX
                );
    }

    Zombie findZombieInTile(int column, int row) {
        for (Zombie zombie : zombies) {
            if (zombie.getTileX() == column
                    && zombie.getTileY() == row) {
                return zombie;
            }
        }

        return null;
    }
}
