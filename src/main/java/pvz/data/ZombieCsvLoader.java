package pvz.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pvz.model.entity.zombie.ArmorType;
import pvz.model.entity.zombie.ZombieSpec;

public class ZombieCsvLoader {

    private ZombieCsvLoader() {}

    public static ZombieData load(String path) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(path));

        Map<String, ZombieSpec> byName = new HashMap<>();
        Map<String, ZombieSpec> byId = new HashMap<>();

        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",");

            ZombieSpec spec = new ZombieSpec(
                    parts[0],
                    parts[1],
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Double.parseDouble(parts[4]),
                    Integer.parseInt(parts[5]),
                    ArmorType.valueOf(parts[6])
            );

            byName.put(spec.getName(), spec);
            byId.put(spec.getId(), spec);
        }

        return new ZombieData(byName, byId);
    }
}
