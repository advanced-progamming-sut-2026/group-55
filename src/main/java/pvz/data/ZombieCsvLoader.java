package pvz.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import pvz.model.entity.zombie.ArmorSpec;
import pvz.model.entity.zombie.ZombieBehaviorDefinition;
import pvz.model.entity.zombie.ZombieSpec;

public final class ZombieCsvLoader {
    private static final String ARMOR_FILE = "armor_types.csv";
    private static final String BEHAVIOR_FILE = "zombie_behavior_params.csv";

    private ZombieCsvLoader() {}

    public static ZombieData load(String path) throws IOException {
        Path zombiePath = Path.of(path);
        Path directory = zombiePath.toAbsolutePath().getParent();
        Map<String, ArmorSpec> armorSpecs = loadArmorSpecs(
                directory.resolve(ARMOR_FILE)
        );
        Map<String, Map<String, Map<String, String>>> parameters =
                loadBehaviorParameters(directory.resolve(BEHAVIOR_FILE));

        Map<String, ZombieSpec> byName = new LinkedHashMap<>();
        Map<String, ZombieSpec> byId = new LinkedHashMap<>();

        List<Row> rows = readRows(zombiePath, 10);
        for (Row row : rows) {
            List<String> values = row.values();
            List<String> armorIds = splitPipe(values.get(7));
            List<String> behaviorTypes = splitPipe(values.get(8)).stream()
                    .map(value -> value.toUpperCase(Locale.ROOT))
                    .toList();

            for (String armorId : armorIds) {
                if (!armorSpecs.containsKey(armorId)) {
                    throw dataError(row, "unknown armor: " + armorId);
                }
            }

            boolean implemented = switch (values.get(9).toUpperCase(Locale.ROOT)) {
                case "SUPPORTED" -> true;
                case "PLANNED" -> false;
                default -> throw dataError(
                        row, "runtimeStatus must be SUPPORTED or PLANNED"
                );
            };

            ZombieSpec spec;
            try {
                spec = new ZombieSpec(
                        values.get(0), values.get(1),
                        Integer.parseInt(values.get(2)),
                        Integer.parseInt(values.get(3)),
                        Double.parseDouble(values.get(4)),
                        Integer.parseInt(values.get(5)),
                        Integer.parseInt(values.get(6)),
                        armorIds, behaviorTypes, implemented
                );
            } catch (NumberFormatException exception) {
                throw dataError(row, "invalid numeric value");
            }

            String normalizedName = normalize(spec.getName());
            String normalizedId = normalize(spec.getId());
            if (byName.putIfAbsent(normalizedName, spec) != null) {
                throw dataError(row, "duplicate zombie name: " + spec.getName());
            }
            if (byId.putIfAbsent(normalizedId, spec) != null) {
                throw dataError(row, "duplicate zombie id: " + spec.getId());
            }
        }

        Map<String, List<ZombieBehaviorDefinition>> behaviorDefinitions =
                buildBehaviorDefinitions(byId, parameters);
        return new ZombieData(byName, byId, armorSpecs, behaviorDefinitions);
    }

    private static Map<String, ArmorSpec> loadArmorSpecs(Path path)
            throws IOException {
        Map<String, ArmorSpec> specs = new LinkedHashMap<>();
        for (Row row : readRows(path, 4)) {
            List<String> values = row.values();
            String id = values.get(0).toUpperCase(Locale.ROOT);
            ArmorSpec spec;
            try {
                spec = new ArmorSpec(
                        id, values.get(1), Double.parseDouble(values.get(2)),
                        parseBoolean(values.get(3), row)
                );
            } catch (NumberFormatException exception) {
                throw dataError(row, "invalid armor health");
            }
            if (specs.putIfAbsent(id, spec) != null) {
                throw dataError(row, "duplicate armor id: " + id);
            }
        }
        return specs;
    }

    private static Map<String, Map<String, Map<String, String>>>
            loadBehaviorParameters(Path path) throws IOException {
        Map<String, Map<String, Map<String, String>>> result = new HashMap<>();
        for (Row row : readRows(path, 4)) {
            List<String> values = row.values();
            String zombieId = normalize(values.get(0));
            String behavior = values.get(1).toUpperCase(Locale.ROOT);
            String parameter = values.get(2);
            Map<String, String> valuesByParameter = result
                    .computeIfAbsent(zombieId, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(behavior, ignored -> new LinkedHashMap<>());
            if (valuesByParameter.putIfAbsent(parameter, values.get(3)) != null) {
                throw dataError(
                        row, "duplicate behavior parameter: " + parameter
                );
            }
        }
        return result;
    }

    private static Map<String, List<ZombieBehaviorDefinition>>
            buildBehaviorDefinitions(
                    Map<String, ZombieSpec> specs,
                    Map<String, Map<String, Map<String, String>>> parameters
            ) {
        validateBehaviorParameterOwners(specs, parameters);
        Map<String, List<ZombieBehaviorDefinition>> result = new HashMap<>();
        for (Map.Entry<String, ZombieSpec> entry : specs.entrySet()) {
            List<ZombieBehaviorDefinition> definitions = new ArrayList<>();
            Map<String, Map<String, String>> zombieParameters =
                    parameters.getOrDefault(entry.getKey(), Map.of());
            for (String type : entry.getValue().getBehaviorTypes()) {
                definitions.add(new ZombieBehaviorDefinition(
                        type, zombieParameters.getOrDefault(type, Map.of())
                ));
            }
            result.put(entry.getKey(), List.copyOf(definitions));
        }
        return result;
    }

    private static void validateBehaviorParameterOwners(
            Map<String, ZombieSpec> specs,
            Map<String, Map<String, Map<String, String>>> parameters
    ) {
        for (Map.Entry<String, Map<String, Map<String, String>>> entry
                : parameters.entrySet()) {
            ZombieSpec spec = specs.get(entry.getKey());
            if (spec == null) {
                throw new IllegalArgumentException(
                        "behavior parameters reference unknown zombie: "
                                + entry.getKey()
                );
            }
            for (String behavior : entry.getValue().keySet()) {
                if (!spec.getBehaviorTypes().contains(behavior)) {
                    throw new IllegalArgumentException(
                            "behavior parameters for " + spec.getId()
                                    + " reference undeclared behavior: "
                                    + behavior
                    );
                }
            }
        }
    }

    private static List<Row> readRows(Path path, int expectedColumns)
            throws IOException {
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("empty CSV file: " + path);
        }
        List<Row> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            List<String> values = parseCsvLine(line);
            if (values.size() != expectedColumns) {
                throw new IllegalArgumentException(
                        path + ":" + (index + 1) + " expected "
                                + expectedColumns + " columns but got "
                                + values.size()
                );
            }
            rows.add(new Row(path, index + 1, values));
        }
        return rows;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
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
            throw new IllegalArgumentException("unterminated quoted CSV value");
        }
        values.add(current.toString().strip());
        return values;
    }

    private static List<String> splitPipe(String value) {
        if (value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\\|", -1)).stream()
                .map(String::strip)
                .filter(item -> !item.isEmpty())
                .map(item -> item.toUpperCase(Locale.ROOT))
                .toList();
    }

    private static boolean parseBoolean(String value, Row row) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw dataError(row, "invalid boolean: " + value);
        };
    }

    private static IllegalArgumentException dataError(Row row, String message) {
        return new IllegalArgumentException(
                row.path() + ":" + row.lineNumber() + " " + message
        );
    }

    private static String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private record Row(Path path, int lineNumber, List<String> values) {}
}
