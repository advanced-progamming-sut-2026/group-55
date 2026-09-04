package pvz.model.entity.plant.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import pvz.data.PlantCsvLoader;
import pvz.data.PlantData;
import pvz.model.entity.plant.PlantSpec;

class PlantLevelDataTest {
    @Test
    void everyPlantDefinesExactlyOneUpgradeForLevelsTwoThroughFour()
            throws IOException {
        PlantData data = PlantCsvLoader.load("assets/Data/plants.csv");

        assertEquals(69, data.byId().size());
        for (PlantSpec spec : data.byId().values()) {
            assertEquals(3, spec.getLevelUpgrades().size(), spec.getName());
            assertEquals(
                    java.util.List.of(2, 3, 4),
                    spec.getLevelUpgrades().stream()
                            .map(PlantLevelUpgrade::targetLevel)
                            .toList(),
                    spec.getName()
            );
            assertEquals(spec.getLvl2(), spec.getLevelUpgrades().get(0).sourceText());
            assertEquals(spec.getLvl3(), spec.getLevelUpgrades().get(1).sourceText());
            assertEquals(spec.getLvl4(), spec.getLevelUpgrades().get(2).sourceText());
        }
    }

    @Test
    void upgradeCostsIncreaseAtEveryLevel() throws IOException {
        PlantLevelCostTable costs = PlantCsvLoader.load("assets/Data/plants.csv")
                .levelCosts();

        PlantLevelCost levelTwo = costs.forTargetLevel(2);
        PlantLevelCost levelThree = costs.forTargetLevel(3);
        PlantLevelCost levelFour = costs.forTargetLevel(4);

        assertTrue(levelTwo.coins() < levelThree.coins());
        assertTrue(levelThree.coins() < levelFour.coins());
        assertTrue(levelTwo.seedPackets() < levelThree.seedPackets());
        assertTrue(levelThree.seedPackets() < levelFour.seedPackets());
    }
}
