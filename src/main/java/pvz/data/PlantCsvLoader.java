package pvz.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import pvz.model.entity.plant.PlantCategory;
import pvz.model.entity.plant.PlantSpec;
import pvz.model.entity.plant.PlantTag;

public class PlantCsvLoader {

    private PlantCsvLoader() {}

    public static PlantData load(String path) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(path));

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
                    plantFoodEffect, lvl2, lvl3, lvl4, actionInterval, recharge);

            byName.put(spec.getName().toLowerCase(Locale.ROOT), spec);
            byId.put(spec.getId(), spec);
        }

        return new PlantData(Map.copyOf(byName), Map.copyOf(byId));
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