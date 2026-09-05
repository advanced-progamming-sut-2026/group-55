package pvz.model.minigame;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Central registry for minigames exposed through the Travel Log. */
public final class MinigameCatalog {
    public static final String VASE_BREAKER = "vase-breaker";
    public static final String WALL_NUT_BOWLING = "wall-nut-bowling";
    public static final String I_ZOMBIE = "i-zombie";

    private static final int REQUIRED_STAGE_COUNT = 3;

    private final Map<String, MinigameSpec> minigamesById;
    private final List<MinigameSpec> orderedMinigames;

    public MinigameCatalog(Collection<MinigameSpec> minigames) {
        Objects.requireNonNull(minigames, "minigames cannot be null");
        Map<String, MinigameSpec> byId = new LinkedHashMap<>();
        for (MinigameSpec minigame : minigames) {
            Objects.requireNonNull(minigame, "minigame cannot be null");
            MinigameSpec duplicate = byId.putIfAbsent(
                    minigame.id(),
                    minigame
            );
            if (duplicate != null) {
                throw new IllegalArgumentException(
                        "duplicate minigame id: " + minigame.id()
                );
            }
        }
        this.minigamesById = Map.copyOf(byId);
        this.orderedMinigames = List.copyOf(byId.values());
    }

    public static MinigameCatalog createDefault() {
        return new MinigameCatalog(List.of(
                new MinigameSpec(
                        VASE_BREAKER,
                        "Vase Breaker",
                        "Break every vase and survive whatever is hidden inside.",
                        REQUIRED_STAGE_COUNT
                ),
                new MinigameSpec(
                        WALL_NUT_BOWLING,
                        "Wall-nut Bowling",
                        "Use rolling Wall-nuts to clear approaching zombies.",
                        REQUIRED_STAGE_COUNT
                ),
                new MinigameSpec(
                        I_ZOMBIE,
                        "I, Zombie",
                        "Play from the zombie side and break through plant defenses.",
                        REQUIRED_STAGE_COUNT
                )
        ));
    }

    public MinigameSpec find(String minigameId) {
        if (minigameId == null || minigameId.isBlank()) {
            return null;
        }
        return minigamesById.get(MinigameSpec.normalizeId(minigameId));
    }

    public MinigameSpec require(String minigameId) {
        MinigameSpec minigame = find(minigameId);
        if (minigame == null) {
            throw new IllegalArgumentException(
                    "unknown minigame id: " + minigameId
            );
        }
        return minigame;
    }

    public List<MinigameSpec> all() {
        return orderedMinigames;
    }

    public int size() {
        return minigamesById.size();
    }
}
