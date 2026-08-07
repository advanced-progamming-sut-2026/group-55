package pvz.model.entity.zombie;

import pvz.model.entity.zombie.zombies.AllstarZombie;
import pvz.model.entity.zombie.zombies.GargantuarZombie;
import pvz.model.entity.zombie.zombies.NewspaperZombie;

import java.util.Locale;
import java.util.Map;

public class ZombieFactory {

    private final Map<String, ZombieSpec> specs;

    public ZombieFactory(Map<String, ZombieSpec> specs) {
        this.specs = specs;
    }

    public Zombie create(String type) {

        ZombieSpec spec = specs.get(
                type.toLowerCase(Locale.ROOT)
        );

        if (spec == null) {
            return null;
        }

        return switch (spec.getName().toLowerCase()) {

            case "gargantuar" ->
                    new GargantuarZombie(spec, this);

            case "allstar" ->
                new AllstarZombie(spec);

            case "news paper" ->
                    new NewspaperZombie(spec);

            default ->
                    new DefaultZombie(spec);
        };
    }

}
