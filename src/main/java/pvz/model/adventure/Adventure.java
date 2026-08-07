package pvz.model.adventure;

import java.util.List;
import java.util.Objects;

public final class Adventure {

    private final List<AdventureLevel> levels;


    public Adventure(List<AdventureLevel> levels) {
        this.levels = List.copyOf(
                Objects.requireNonNull(levels)
        );
    }


    public List<AdventureLevel> getLevels() {
        return levels;
    }


    public AdventureLevel getLevel(int number) {

        return levels.stream()
                .filter(level ->
                        level.getNumber() == number
                )
                .findFirst()
                .orElse(null);
    }
}
