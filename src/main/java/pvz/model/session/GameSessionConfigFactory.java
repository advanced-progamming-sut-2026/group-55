package pvz.model.session;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import pvz.model.adventure.AdventureData;
import pvz.model.adventure.LevelSpec;
import pvz.model.session.condition.WinConditionFactory;
import pvz.model.wave.WaveConfiguration;

public final class GameSessionConfigFactory {
    private final AdventureData adventureData;
    private final WinConditionFactory winConditionFactory;

    public GameSessionConfigFactory(AdventureData adventureData) {
        this(adventureData, new WinConditionFactory());
    }

    GameSessionConfigFactory(
            AdventureData adventureData,
            WinConditionFactory winConditionFactory
    ) {
        this.adventureData = Objects.requireNonNull(
                adventureData,
                "adventure data cannot be null"
        );
        this.winConditionFactory = Objects.requireNonNull(
                winConditionFactory,
                "win condition factory cannot be null"
        );
    }

    public GameSessionConfig create(
            String levelId,
            List<String> selectedPlants,
            Set<String> boostedPlants,
            int startingPlantFood,
            int difficultyLevel
    ) {
        return create(
                levelId, selectedPlants, Map.of(), boostedPlants,
                startingPlantFood, difficultyLevel
        );
    }

    public GameSessionConfig create(
            String levelId,
            List<String> selectedPlants,
            Map<String, Integer> plantLevels,
            Set<String> boostedPlants,
            int startingPlantFood,
            int difficultyLevel
    ) {
        LevelSpec level = adventureData.catalog().requireLevel(levelId);
        String normalizedLevelId = level.id()
                .toLowerCase(Locale.ROOT);
        Map<String, WaveConfiguration> configurations =
                adventureData.wavesByLevelId();
        WaveConfiguration waves = configurations.get(normalizedLevelId);
        if (waves == null) {
            throw new IllegalStateException(
                    "missing wave configuration for level: " + level.id()
            );
        }

        return new GameSessionConfig.Builder(
                level.id(),
                selectedPlants
        )
                .columns(level.columns())
                .rows(level.rows())
                .startingSun(level.startingSun())
                .startingPlantFood(startingPlantFood)
                .difficultyLevel(difficultyLevel)
                .skySunEnabled(level.skySunEnabled())
                .plantLevels(plantLevels)
                .boostedPlants(boostedPlants)
                .waveConfiguration(waves)
                .winCondition(
                        winConditionFactory.create(level.objectiveType())
                )
                .build();
    }
}
