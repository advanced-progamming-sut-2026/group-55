package pvz.model.core;

import pvz.model.entity.zombie.ZombieSpec;

@FunctionalInterface
public interface ZombieDiscoveryListener {
    void onZombieDiscovered(ZombieSpec zombieSpec);

    static ZombieDiscoveryListener none() {
        return zombieSpec -> {
        };
    }
}
