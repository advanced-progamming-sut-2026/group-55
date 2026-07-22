package pvz.model.session;

import java.util.*;
import java.util.stream.Collectors;

public record GameSessionConfig(
        String levelId,
        int columns,
        int rows,
        int startingSun,
        int difficultyLevel,
        boolean skySunEnabled,
        List<String> selectedPlants,
        Set<String> boostedPlants
) {
    public GameSessionConfig {
        levelId = requireNonBlank(levelId, "level id");

        if (columns <= 0 || columns > 9) {
            throw new IllegalArgumentException("Columns must be between 1 and 9, but got: " + columns);
        }
        if (rows <= 0 || rows > 5) {
            throw new IllegalArgumentException("Rows must be between 1 and 5, but got: " + rows);
        }
        if (startingSun < 0) {
            throw new IllegalArgumentException("starting sun cannot be negative");
        }
        if (difficultyLevel < 1 || difficultyLevel > 5) {
            throw new IllegalArgumentException("difficulty level must be between 1 and 5");
        }

        Objects.requireNonNull(selectedPlants, "selected plants cannot be null");
        Objects.requireNonNull(boostedPlants, "boosted plants cannot be null");

        selectedPlants = selectedPlants.stream()
                .map(GameSessionConfig::normalizePlantName)
                .distinct()
                .toList();

        boostedPlants = boostedPlants.stream()
                .map(GameSessionConfig::normalizePlantName)
                .collect(Collectors.toUnmodifiableSet());

        if (!new HashSet<>(selectedPlants).containsAll(boostedPlants)) {
            throw new IllegalArgumentException("every boosted plant must also be selected");
        }
    }
    public static class Builder {
        private final String levelId;
        private final List<String> selectedPlants;

        private int columns = 9;
        private int rows = 5;
        private int startingSun = 50;
        private int difficultyLevel = 3;
        private boolean skySunEnabled = true;
        private Set<String> boostedPlants = Set.of();

        public Builder(
                String levelId,
                List<String> selectedPlants
        ) {
            this.levelId = levelId;
            this.selectedPlants = selectedPlants;
        }

        public Builder columns(int columns) {
            this.columns = columns;
            return this;
        }

        public Builder rows(int rows) {
            this.rows = rows;
            return this;
        }

        public Builder startingSun(int startingSun) {
            this.startingSun = startingSun;
            return this;
        }

        public Builder difficultyLevel(int difficultyLevel) {
            this.difficultyLevel = difficultyLevel;
            return this;
        }

        public Builder skySunEnabled(boolean enabled) {
            this.skySunEnabled = enabled;
            return this;
        }

        public Builder nightMode() {
            return skySunEnabled(false);
        }

        public Builder boostedPlants(
                Set<String> boostedPlants
        ) {
            this.boostedPlants = boostedPlants;
            return this;
        }

        public GameSessionConfig build() {
            return new GameSessionConfig(
                    levelId,
                    columns,
                    rows,
                    startingSun,
                    difficultyLevel,
                    skySunEnabled,
                    selectedPlants,
                    boostedPlants
            );
        }
    }

    private static String normalizePlantName(String plantName) {
        return requireNonBlank(plantName, "plant name")
                .toLowerCase(Locale.ROOT);
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null");
        String stripped = value.strip();
        if (stripped.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return stripped;
    }
}
