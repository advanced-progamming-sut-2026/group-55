package pvz.model.entity.zombie;

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


        return new DefaultZombie(spec);
    }
}
