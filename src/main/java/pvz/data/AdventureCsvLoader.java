package pvz.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import pvz.model.adventure.AdventureData;
import pvz.model.adventure.ChapterSpec;
import pvz.model.adventure.LevelCatalog;
import pvz.model.adventure.LevelSpec;
import pvz.model.adventure.LevelType;
import pvz.model.adventure.ObjectiveType;
import pvz.model.core.Game;
import pvz.model.entity.zombie.ZombieSpec;
import pvz.model.wave.WaveConfig;
import pvz.model.wave.WaveConfiguration;

public final class AdventureCsvLoader {
    private AdventureCsvLoader() {
    }

    public static AdventureData load(
            String chaptersPath,
            String levelsPath,
            String levelZombiesPath,
            String wavesPath,
            ZombieData zombieData
    ) throws IOException {
        Map<String, ChapterSpec> chapters = loadChapters(
                Path.of(chaptersPath)
        );
        Map<String, LevelSpec> levels = loadLevels(
                Path.of(levelsPath),
                chapters
        );
        Map<String, List<String>> zombieIdsByLevel = loadLevelZombies(
                Path.of(levelZombiesPath),
                levels,
                zombieData
        );
        Map<String, List<WaveConfig>> wavesByLevel = loadWaves(
                Path.of(wavesPath),
                levels
        );

        Map<String, WaveConfiguration> configurations =
                new LinkedHashMap<>();
        for (String levelId : levels.keySet()) {
            List<String> zombieIds = zombieIdsByLevel.get(levelId);
            List<WaveConfig> waves = wavesByLevel.get(levelId);
            if (zombieIds == null) {
                throw new IllegalArgumentException(
                        "level has no zombie pool: " + levelId
                );
            }
            if (waves == null) {
                throw new IllegalArgumentException(
                        "level has no wave configuration: " + levelId
                );
            }
            configurations.put(
                    levelId,
                    new WaveConfiguration(zombieIds, waves)
            );
        }

        return new AdventureData(
                new LevelCatalog(chapters, levels),
                configurations
        );
    }

    private static Map<String, ChapterSpec> loadChapters(Path path)
            throws IOException {
        CsvFile csv = read(path, List.of("id", "name", "order"));
        Map<String, ChapterSpec> result = new LinkedHashMap<>();
        Map<Integer, String> idsByOrder = new LinkedHashMap<>();
        for (Row row : csv.rows()) {
            ChapterSpec spec;
            try {
                spec = new ChapterSpec(
                        row.value(0),
                        row.value(1),
                        Integer.parseInt(row.value(2))
                );
            } catch (NumberFormatException exception) {
                throw row.error("invalid chapter order");
            }
            String id = normalize(spec.id());
            if (result.putIfAbsent(id, spec) != null) {
                throw row.error("duplicate chapter id: " + spec.id());
            }
            if (idsByOrder.putIfAbsent(spec.order(), id) != null) {
                throw row.error("duplicate chapter order: " + spec.order());
            }
        }
        return result;
    }

    private static Map<String, LevelSpec> loadLevels(
            Path path,
            Map<String, ChapterSpec> chapters
    ) throws IOException {
        CsvFile csv = read(path, List.of(
                "id", "chapterId", "number", "name", "type",
                "columns", "rows", "startingSun", "skySunEnabled",
                "objectiveType"
        ));
        Map<String, LevelSpec> result = new LinkedHashMap<>();
        Map<String, String> levelByChapterAndNumber = new LinkedHashMap<>();
        for (Row row : csv.rows()) {
            LevelSpec spec;
            try {
                spec = new LevelSpec(
                        row.value(0),
                        row.value(1),
                        Integer.parseInt(row.value(2)),
                        row.value(3),
                        LevelType.valueOf(row.value(4).toUpperCase(Locale.ROOT)),
                        Integer.parseInt(row.value(5)),
                        Integer.parseInt(row.value(6)),
                        Integer.parseInt(row.value(7)),
                        parseBoolean(row.value(8), row),
                        ObjectiveType.fromCsv(row.value(9))
                );
            } catch (NumberFormatException exception) {
                throw row.error("invalid numeric level value");
            } catch (IllegalArgumentException exception) {
                throw row.error(exception.getMessage());
            }
            String id = normalize(spec.id());
            String chapterId = normalize(spec.chapterId());
            if (!chapters.containsKey(chapterId)) {
                throw row.error("unknown chapter id: " + spec.chapterId());
            }
            if (spec.type() != LevelType.NORMAL) {
                throw row.error(
                        "only NORMAL levels are supported by this slice"
                );
            }
            if (result.putIfAbsent(id, spec) != null) {
                throw row.error("duplicate level id: " + spec.id());
            }
            String chapterNumberKey = chapterId + ":" + spec.number();
            if (levelByChapterAndNumber.putIfAbsent(
                    chapterNumberKey,
                    id
            ) != null) {
                throw row.error(
                        "duplicate level number inside chapter: "
                                + spec.number()
                );
            }
        }
        return result;
    }

    private static Map<String, List<String>> loadLevelZombies(
            Path path,
            Map<String, LevelSpec> levels,
            ZombieData zombieData
    ) throws IOException {
        CsvFile csv = read(path, List.of("levelId", "zombieId"));
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Row row : csv.rows()) {
            String levelId = normalize(row.value(0));
            String zombieId = normalize(row.value(1));
            if (!levels.containsKey(levelId)) {
                throw row.error("unknown level id: " + row.value(0));
            }
            ZombieSpec zombie = zombieData.byId().get(zombieId);
            if (zombie == null) {
                throw row.error("unknown zombie id: " + row.value(1));
            }
            if (!zombie.isImplemented()) {
                throw row.error(
                        "planned zombie cannot be used in a level: "
                                + zombie.getId()
                );
            }
            List<String> ids = result.computeIfAbsent(
                    levelId,
                    ignored -> new ArrayList<>()
            );
            if (ids.contains(zombie.getId())) {
                throw row.error(
                        "duplicate zombie in level pool: " + zombie.getId()
                );
            }
            ids.add(zombie.getId());
        }
        result.replaceAll((ignored, ids) -> List.copyOf(ids));
        return result;
    }

    private static Map<String, List<WaveConfig>> loadWaves(
            Path path,
            Map<String, LevelSpec> levels
    ) throws IOException {
        CsvFile csv = read(path, List.of(
                "levelId", "waveNumber", "budget", "startDelaySeconds",
                "spawnIntervalSeconds", "flagWave"
        ));
        Map<String, List<WaveConfig>> result = new LinkedHashMap<>();
        for (Row row : csv.rows()) {
            String levelId = normalize(row.value(0));
            if (!levels.containsKey(levelId)) {
                throw row.error("unknown level id: " + row.value(0));
            }
            WaveConfig wave;
            try {
                wave = new WaveConfig(
                        Integer.parseInt(row.value(1)),
                        Integer.parseInt(row.value(2)),
                        secondsToTicks(row.value(3), row),
                        secondsToTicks(row.value(4), row),
                        parseBoolean(row.value(5), row)
                );
            } catch (NumberFormatException exception) {
                throw row.error("invalid numeric wave value");
            }
            List<WaveConfig> waves = result.computeIfAbsent(
                    levelId,
                    ignored -> new ArrayList<>()
            );
            if (waves.stream().anyMatch(
                    existing -> existing.number() == wave.number()
            )) {
                throw row.error(
                        "duplicate wave number: " + wave.number()
                );
            }
            waves.add(wave);
        }
        result.replaceAll((ignored, waves) -> waves.stream()
                .sorted((first, second) -> Integer.compare(
                        first.number(),
                        second.number()
                ))
                .toList());
        return result;
    }

    private static long secondsToTicks(String value, Row row) {
        double seconds;
        try {
            seconds = Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw row.error("invalid seconds value: " + value);
        }
        if (seconds < 0 || !Double.isFinite(seconds)) {
            throw row.error("seconds value cannot be negative");
        }
        return (long) Math.ceil(seconds * Game.TICKS_PER_SECOND);
    }

    private static boolean parseBoolean(String value, Row row) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw row.error("invalid boolean: " + value);
        };
    }

    private static CsvFile read(Path path, List<String> expectedHeader)
            throws IOException {
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("empty CSV file: " + path);
        }
        List<String> header = parseCsvLine(lines.get(0));
        if (!header.equals(expectedHeader)) {
            throw new IllegalArgumentException(
                    path + ":1 expected header " + expectedHeader
                            + " but got " + header
            );
        }
        List<Row> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index).isBlank()) {
                continue;
            }
            List<String> values = parseCsvLine(lines.get(index));
            if (values.size() != expectedHeader.size()) {
                throw new IllegalArgumentException(
                        path + ":" + (index + 1) + " expected "
                                + expectedHeader.size() + " columns but got "
                                + values.size()
                );
            }
            rows.add(new Row(path, index + 1, values));
        }
        return new CsvFile(rows);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length()
                        && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString().strip());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException(
                    "unterminated quoted CSV value"
            );
        }
        values.add(current.toString().strip());
        return values;
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private record CsvFile(List<Row> rows) {
    }

    private record Row(Path path, int lineNumber, List<String> values) {
        String value(int index) {
            return values.get(index);
        }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(
                    path + ":" + lineNumber + " " + message
            );
        }
    }
}
