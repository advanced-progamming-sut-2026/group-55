package pvz.model.special;

import java.util.List;
import java.util.Objects;

import pvz.model.wave.Wave;

public final class SpecialLevel {

    private final int number;
    private final String name;
    private final SpecialLevelType type;
    private final List<Wave> waves;


    public SpecialLevel(
            int number,
            String name,
            SpecialLevelType type,
            List<Wave> waves
    ) {
        if (number <= 0) {
            throw new IllegalArgumentException(
                    "level number must be positive"
            );
        }

        this.number = number;
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        this.waves = List.copyOf(waves);
    }


    public int getNumber() {
        return number;
    }


    public String getName() {
        return name;
    }


    public SpecialLevelType getType() {
        return type;
    }


    public List<Wave> getWaves() {
        return waves;
    }
}
