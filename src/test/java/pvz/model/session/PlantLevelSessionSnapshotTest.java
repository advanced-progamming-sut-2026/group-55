package pvz.model.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import pvz.data.AdventureCsvLoader;
import pvz.data.PlantCsvLoader;
import pvz.data.ZombieCsvLoader;
import pvz.data.ZombieData;
import pvz.model.adventure.AdventureData;
import pvz.model.entity.plant.Plant;
import pvz.model.core.board.TileType;
import pvz.model.entity.plant.PlantFactory;
import pvz.model.entity.zombie.ZombieFactory;

class PlantLevelSessionSnapshotTest {
    @Test
    void selectedPlantLevelIsFrozenIntoSessionAndUsedForAbilitySpawns()
            throws IOException {
        ZombieData zombieData = ZombieCsvLoader.load("assets/Data/zombies.csv");
        AdventureData adventureData = AdventureCsvLoader.load(
                "assets/Data/chapters.csv",
                "assets/Data/levels.csv",
                "assets/Data/level_zombies.csv",
                "assets/Data/waves.csv",
                zombieData
        );
        PlantFactory plantFactory = new PlantFactory(
                PlantCsvLoader.load("assets/Data/plants.csv").byName()
        );
        GameSessionConfig config = new GameSessionConfigFactory(adventureData).create(
                "egypt-1",
                List.of("Lily Pad", "Peashooter"),
                Map.of("Lily Pad", 3, "Peashooter", 4),
                Set.of(),
                0,
                3
        );
        GameSession session = new GameSessionFactory(
                plantFactory,
                new ZombieFactory(zombieData)
        ).create(config);
        session.start();

        Plant peashooter = session.createPlant("Peashooter");
        assertEquals(4, peashooter.getSpec().getLevel());
        assertEquals(30, Double.parseDouble(peashooter.getSpec().getDamage()));
        assertEquals(75, peashooter.getSpec().getCost());

        session.world().board().setTileType(2, 2, TileType.WATER);
        Plant abilitySpawn = session.world().spawnPlantFromAbility("Lily Pad", 2, 2);
        assertEquals(3, abilitySpawn.getSpec().getLevel());
        assertEquals(500, abilitySpawn.getSpec().getBaseHp());
    }
}
