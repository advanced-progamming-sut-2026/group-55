package pvz.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.PlantTag;
import pvz.model.entity.plant.level.PlantLevelCost;
import pvz.model.entity.plant.level.PlantLevelCostTable;
import pvz.model.entity.plant.level.PlantLevelUpgrade;
import pvz.model.entity.plant.level.PlantUpgradeType;

public class PlantCsvLoader {

    private static final String BEHAVIOR_PARAMS_FILE =
            "plant_behavior_params.csv";

    private static final int BEHAVIOR_PARAMS_COLUMNS = 4;
    private static final String LEVEL_UPGRADES_FILE = "plant_level_upgrades.csv";
    private static final String LEVEL_COSTS_FILE = "plant_level_costs.csv";
    private static final int LEVEL_UPGRADE_COLUMNS = 5;
    private static final int LEVEL_COST_COLUMNS = 3;

    private PlantCsvLoader() {}

    public static PlantData load(String path) throws IOException {
        Path plantPath = Path.of(path);
        List<String> lines = Files.readAllLines(plantPath);

        Map<Integer, Map<String, Map<String, Double>>> behaviorParams =
                loadBehaviorParams(plantPath);
        Map<Integer, List<PlantLevelUpgrade>> levelUpgrades =
                loadLevelUpgrades(plantPath);
        PlantLevelCostTable levelCosts = loadLevelCosts(plantPath);

        Map<String, PlantSpec> byName = new HashMap<>();
        Map<Integer, PlantSpec> byId = new HashMap<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            String[] parts = splitCsvLine(line);

            if (parts.length != 14) {
                throw new IllegalArgumentException(
                        "Bad line " + (i + 1) + ": expected 14 columns, got " + parts.length
                );
            }

            int id = Integer.parseInt(parts[0]);
            String name = parts[1];
            PlantCategory category = PlantCategory.valueOf(parts[2]);
            Set<PlantTag> tags = parseTags(parts[3]);
            int cost = Integer.parseInt(parts[4]);
            int baseHp = Integer.parseInt(parts[5]);
            String damage = parts[6];
            String baseAbility = parts[7];
            String plantFoodEffect = parts[8];
            String lvl2 = parts[9];
            String lvl3 = parts[10];
            String lvl4 = parts[11];
            double actionInterval = parseActionInterval(parts[12]);
            double recharge = Double.parseDouble(parts[13]);
            PlantSpec spec = new PlantSpec(id, name, category, tags, cost, baseHp, damage, baseAbility,
                    plantFoodEffect, lvl2, lvl3, lvl4, actionInterval, recharge,
                    behaviorParams.getOrDefault(id, Map.of()),
                    levelUpgrades.getOrDefault(id, List.of()));
            validateLevelUpgradeCoverage(spec);

            byName.put(spec.getName().toLowerCase(Locale.ROOT), spec);
            byId.put(spec.getId(), spec);
        }

        validateNoUnknownUpgradePlantIds(levelUpgrades, byId.keySet());
        return new PlantData(Map.copyOf(byName), Map.copyOf(byId), levelCosts);
    }


    private static Map<Integer, List<PlantLevelUpgrade>> loadLevelUpgrades(Path plantPath)
            throws IOException {
        Path directory = plantPath.toAbsolutePath().getParent();
        if (directory == null) {
            return Map.of();
        }
        Path upgradePath = directory.resolve(LEVEL_UPGRADES_FILE);
        if (!Files.exists(upgradePath)) {
            return Map.of();
        }

        Map<Integer, List<PlantLevelUpgrade>> result = new HashMap<>();
        List<String> lines = Files.readAllLines(upgradePath);
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index).strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = splitCsvLine(line);
            if (parts.length != LEVEL_UPGRADE_COLUMNS) {
                throw new IllegalArgumentException(
                        "Bad plant level upgrade line " + (index + 1)
                                + ": expected " + LEVEL_UPGRADE_COLUMNS
                                + " columns, got " + parts.length
                );
            }
            int plantId = Integer.parseInt(parts[0].strip());
            int targetLevel = Integer.parseInt(parts[1].strip());
            PlantUpgradeType type = PlantUpgradeType.valueOf(parts[2].strip());
            double value = Double.parseDouble(parts[3].strip());
            String sourceText = parts[4].strip();
            PlantLevelUpgrade upgrade = new PlantLevelUpgrade(
                    targetLevel, type, value, sourceText);
            List<PlantLevelUpgrade> upgrades =
                    result.computeIfAbsent(plantId, ignored -> new ArrayList<>());
            if (upgrades.stream().anyMatch(existing -> existing.targetLevel() == targetLevel)) {
                throw new IllegalArgumentException(
                        "duplicate plant upgrade for plant " + plantId
                                + " target level " + targetLevel
                );
            }
            upgrades.add(upgrade);
        }
        result.replaceAll((ignored, upgrades) -> upgrades.stream()
                .sorted(Comparator.comparingInt(PlantLevelUpgrade::targetLevel))
                .toList());
        return Map.copyOf(result);
    }

    private static PlantLevelCostTable loadLevelCosts(Path plantPath) throws IOException {
        Path directory = plantPath.toAbsolutePath().getParent();
        if (directory == null) {
            return PlantLevelCostTable.defaults();
        }
        Path costPath = directory.resolve(LEVEL_COSTS_FILE);
        if (!Files.exists(costPath)) {
            return PlantLevelCostTable.defaults();
        }

        Map<Integer, PlantLevelCost> costs = new HashMap<>();
        List<String> lines = Files.readAllLines(costPath);
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index).strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = splitCsvLine(line);
            if (parts.length != LEVEL_COST_COLUMNS) {
                throw new IllegalArgumentException(
                        "Bad plant level cost line " + (index + 1)
                                + ": expected " + LEVEL_COST_COLUMNS
                                + " columns, got " + parts.length
                );
            }
            PlantLevelCost cost = new PlantLevelCost(
                    Integer.parseInt(parts[0].strip()),
                    Integer.parseInt(parts[1].strip()),
                    Integer.parseInt(parts[2].strip())
            );
            if (costs.putIfAbsent(cost.targetLevel(), cost) != null) {
                throw new IllegalArgumentException(
                        "duplicate plant level cost for target level " + cost.targetLevel());
            }
        }
        return new PlantLevelCostTable(costs);
    }

    private static void validateLevelUpgradeCoverage(PlantSpec spec) {
        if (spec.getLevelUpgrades().isEmpty()) {
            return;
        }
        if (spec.getLevelUpgrades().size() != 3) {
            throw new IllegalArgumentException(
                    "plant " + spec.getId() + " must define exactly three level upgrades");
        }
        String[] sourceTexts = {spec.getLvl2(), spec.getLvl3(), spec.getLvl4()};
        for (int targetLevel = 2; targetLevel <= 4; targetLevel++) {
            final int expectedLevel = targetLevel;
            PlantLevelUpgrade upgrade = spec.getLevelUpgrades().stream()
                    .filter(candidate -> candidate.targetLevel() == expectedLevel)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "plant " + spec.getId() + " missing level " + expectedLevel
                                    + " upgrade"));
            String expectedText = sourceTexts[targetLevel - 2].strip();
            if (!upgrade.sourceText().equals(expectedText)) {
                throw new IllegalArgumentException(
                        "plant " + spec.getId() + " level " + targetLevel
                                + " upgrade source text mismatch: expected '" + expectedText
                                + "' but got '" + upgrade.sourceText() + "'"
                );
            }
        }
    }

    private static void validateNoUnknownUpgradePlantIds(
            Map<Integer, List<PlantLevelUpgrade>> upgrades,
            Set<Integer> knownPlantIds
    ) {
        for (Integer plantId : upgrades.keySet()) {
            if (!knownPlantIds.contains(plantId)) {
                throw new IllegalArgumentException(
                        "plant level upgrade references unknown plant id " + plantId);
            }
        }
    }

    private static Map<Integer, Map<String, Map<String, Double>>>
            loadBehaviorParams(Path plantPath) throws IOException {
        Path directory = plantPath.toAbsolutePath().getParent();

        if (directory == null) {
            return Map.of();
        }

        Path paramsPath = directory.resolve(BEHAVIOR_PARAMS_FILE);

        if (!Files.exists(paramsPath)) {
            return Map.of();
        }

        Map<Integer, Map<String, Map<String, Double>>> result =
                new HashMap<>();

        List<String> lines = Files.readAllLines(paramsPath);

        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index).strip();

            if (line.isEmpty()) {
                continue;
            }

            addBehaviorParam(result, splitCsvLine(line), index + 1);
        }

        return Map.copyOf(result);
    }

    private static void addBehaviorParam(
            Map<Integer, Map<String, Map<String, Double>>> result,
            String[] parts,
            int lineNumber
    ) {
        if (parts.length != BEHAVIOR_PARAMS_COLUMNS) {
            throw new IllegalArgumentException(
                    "Bad behavior param line " + lineNumber
                            + ": expected " + BEHAVIOR_PARAMS_COLUMNS
                            + " columns, got " + parts.length
            );
        }

        int plantId = Integer.parseInt(parts[0].strip());
        String behavior = parts[1].strip().toUpperCase(Locale.ROOT);
        String param = parts[2].strip();
        double value = Double.parseDouble(parts[3].strip());

        Map<String, Double> values = result
                .computeIfAbsent(plantId, ignored -> new HashMap<>())
                .computeIfAbsent(behavior, ignored -> new HashMap<>());

        if (values.putIfAbsent(param, value) != null) {
            throw new IllegalArgumentException(
                    "duplicate behavior param " + param
                            + " for plant " + plantId
            );
        }
    }

    private static Set<PlantTag> parseTags(String column) {
        Set<PlantTag> tags = new HashSet<>();
        if (column.isEmpty()) {
            return tags;
        }
        for (String piece : column.split("\\|")) {
            tags.add(PlantTag.valueOf(piece));
        }
        return tags;
    }

    private static double parseActionInterval(String column) {
        if (column.equals("-")) {
            return 0;
        }
        return Double.parseDouble(column);
    }

    private static String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                }
                else {
                    inQuotes = !inQuotes;
                }
            }
            else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            }
            else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(String[] :: new);
    }
}